package psl.survivor.proc;

import java.util.Set;
import java.util.Date;
import java.util.Vector;
import java.util.TreeSet;
import java.util.ArrayList;

import java.net.InetAddress;
import java.net.UnknownHostException;

import psl.survivor.net.*;
import psl.survivor.util.*;


/**
 * A Processors capable of executing tasks.
 * Processors often have Replicators which are critical in replicating
 * workflow data in order to provide for a survivable system. 
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public abstract class Processor implements Runnable {

    /** Processor Log */
    protected psl.survivor.proc.Log _log;

    /** Name of processor */
    protected String _processorName;

    /** Port that the WVM of this processor is running on */
    protected int _tcpPort;

    /** Path of Workflow data that this processor needs to know about */
    protected String _wfDefPath;

    /** Name of this processor for the rmi registry */
    protected String _rmiName;

    /** Hostname of this processor */
    protected String _hostname;

    private Set _stoppedTasks; // tasks that have been stopped

    /** Partial executions of the workflow that we know about. This is used
	to restart workflows post crash */
    private VersionCache _versionCache; 

    /** Tasks that need to be executed locally */
    private Vector _taskQueue;

    /** Replicators associated with this Processor */
    private Vector _replicatorQueue;

    /** Information about other remote Processors that we know about */
    private PoolData _poolData;

    /** What this processor is able to handle */
    protected ArrayList _capabilities;

    /** Information about the global workflow this processor 
	may be executing */
    protected WorkflowData _workflowData;

    /** Used to communicate with other Processors */
    private MessageHandler _messageHandler;

    /** Main replicator of this Processors */
    protected Replicator _replicator = null;

    /** Whether this Processor has a main replicator */
    protected boolean _hasReplicator = false;

    // SLEEP TIMES ASSOCIATED WITH THE TASK PROCESSING LOOP
    private int _SLEEP_TIME = 500;
    private int _SLEEP_TIME2 = 1000;

    /**
     * CTOR
     */
    public Processor(String name, int tcpPort, String rmiName, 
		     String wfDefPath, Log l) {
	_log = l;
	_stoppedTasks = new TreeSet();
	_versionCache = new VersionCache();
	_taskQueue = new Vector();
	_replicatorQueue = new Vector();
	_poolData = new PoolData(this);
	_capabilities = new ArrayList();
	_processorName = name;
	_tcpPort = tcpPort;
	_wfDefPath = wfDefPath;
	_rmiName = rmiName;
	try {
	    _hostname = InetAddress.getLocalHost().getHostName();
	} catch (UnknownHostException e) {
	    log(e.toString());
	    _hostname = "";
	}

	// grab workflow information from the workflow's xml and config files
	_workflowData = new WorkflowData(_wfDefPath);
    }

    public void setMessageHandler(MessageHandler mh) { _messageHandler = mh; }
    public MessageHandler getMessageHandler() { return _messageHandler; }
    public PoolData getPoolData() { return _poolData; }
    public String getName() { return _processorName; }
    public int getPort() { return _tcpPort; }
    public String getWfDefPath() { return _wfDefPath; }
    public String getRmiName() { return _rmiName; }
    public String getHostName() { return _hostname; }
    public Log getLog() { return _log; }

    /** Returns a handle to this Task Processor. This handle may be used 
     * Remotely to communicate with this Processor */
    public TaskProcessorHandle getHandle() {
	return new TaskProcessorHandle(this);
    }

    /** Add the replicator that works in conjunction with this processor */
    public void addMainReplicator(Replicator r) {
	_replicator = r;
	_hasReplicator = true;
	r.setPoolData(_poolData);
	r.setVersionCache(_versionCache);
	
	// THE FOLLOWING TWO LINES USED TO BE IN THE CONSTRUCTOR
	// THE PROBLEM IS THAT WE CAN'T DO THIS UNTIL WE KNOW ABOUT 
	// THE REPLICATING CAPABILITIES OF THE PROCESSOR
	TaskProcessorHandle tph = new TaskProcessorHandle(this);
       	addProcessor(tph);
    }

    /** Whether or not this processor has replicating abilities */
    public boolean hasMainReplicator() { return _hasReplicator; }
    public Replicator getMainReplicator() { return _replicator; }


    /** Add a remote replicator that this processor may use to
     * guarantee that the workflow has some level of survivability */
    public void addReplicator(ReplicatorHandle rh) {
	synchronized(_replicatorQueue) {
	    _replicatorQueue.add(rh);
	}
    }

    /** Add a capability to what this Processor is able to do */
    public void addCapability(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	    _log.addedCapability(o);
	}
    }

    /** Get a list of actions that this processor may handle */
    public ArrayList getCapabilities() {
	return _capabilities;
    }


    /** Get a list of remote replicators that this processor may use
     * to guarantee workflow survivability */
    public Vector getReplicators() { return _replicatorQueue; }


    /** Add a remote Processor to our Pool of available processors */
    public void addProcessor(TaskProcessorHandle tph) {
	_poolData.addProcessor(tph);
	if (tph.hasMainReplicator()) {
	    ReplicatorHandle rh = tph.getReplicatorHandle();
	    if (!rh.getName().equals(_replicator.getName())) {
		addReplicator(rh);
	    }
	}
    }

    /** Add multiple processors to our Pool of available processors */
    public void addPool(ArrayList al) {
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		addProcessor((TaskProcessorHandle)al.get(i));
	    }
	}
    }

    /** Add a new handle to our knowledge base. Make sure to send that
     * handle a list of processors that we know about */
    public void alertNewHandle(TaskProcessorHandle tph) {
	log(" alert new Handle ");
	ArrayList al = _poolData.getProcessors();
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		TaskProcessorHandle t = (TaskProcessorHandle) al.get(i);
		if (t==null)  {
		    if (psl.survivor.ProcessorMain.debug) System.out.println("t == null");
		} else {
		    if (psl.survivor.ProcessorMain.debug) System.out.println("t.sendNewHandle");
		    t.sendNewHandle(tph, this); // tell new handle about us
		}
	    }
	    addProcessor(tph); // add to local pool
	    if (psl.survivor.ProcessorMain.debug) System.out.println("tph.sendPool");
	    tph.sendPool(al, this); // tell new handle about other processors
	}
    }

    /**
     * Given information about a task, execute it.
     */
    public void executeTask(Version theTask) {

	if (_stoppedTasks.contains(theTask)) {
	    // todo: what to do here???
	    // this task obviously didn't perform well .. 
	    // and now, there's another request to run it!
	    if (true) {
		// either try the task again ... 
		_stoppedTasks.remove(theTask);
	    } else {
		// or, refuse to run it at all 
		return;
	    }
	}
	
	//        System.out.println(" - - - - - - - - - - - PSL! entered executeTask, parameter version>  " + theTask);
	// if (_versionCache.contains(theTask)) return;


	// add the pre-execution task to our knowledge base
	// replicate it so that in case of a crash we may recover
	_versionCache.addVersion(theTask);
	replicate(theTask);

	// tell replicators that we are about to start executing
	alertReplicatorsExecutingTask(theTask);

	String taskName = ((TaskDefinition)theTask.data()).getName();

	// execute the task and get the next task
	Version nextTask = executeTaskLocal(theTask);
	_log.completedTaskLocal(theTask);

	if (_stoppedTasks.contains(theTask)) {
	    // this task was supposed to be stopped .. can ignore result
	    _stoppedTasks.remove(theTask);
	    _log.ignoreResultsOfStoppedTask(theTask);
	} else if (nextTask == null) { 
	    // this only happens if we have reached
	    // the end of the workflow execution 
	    // tell replicators that everything is done.
	    System.out.println("WORKFLOW IS DONE");
	    alertReplicatorsDoneExecutingTask(theTask);
	} else {
	    nextTask.append(taskName);
	    // todo: we need to find a way to replicate the 
	    // post-execution task in a clean way. The here-under
	    // commented code does not work as it creates a version clash.
	    // todo: 16-Feb maybe uncomment this??
	    // _versionCache.addVersion(nextTask);

	    // set the next task for execution
	    queueTask(nextTask);
	    // tell the replicators we are done executing the task, they
	    // don't have to worry about things anymore.
	    alertReplicatorsDoneExecutingTask(theTask);
	}
    }

    public void stopTask(Version v) {
	//	System.err.println(" ##############################################################################################################################################################\nSTOP TASK WOULD BE HAPPENING NOW\n"+v+"\n##############################################################################################################################################################");
	// WE NEED TO FIGURE OUT HOW TO DO THIS PROPERLY
	// RIGHT NOW, CAN WE STOP A RUNNING TASK?
	
	// No way to stop a running thread is there (deprecated)
	// So currently, we let the task execute, but when it ends, we do
	// not pass its results to anyone.
	_stoppedTasks.add(v);
	_log.stopTaskLocal(v);
    }

    /** Tell the replicators that we are about to execute a Task. */
    private void alertReplicatorsExecutingTask(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.alertExecutingTask(theTask, this);
	    }
	}	
    }

    /** Tell the replicators that we are done executing a task, they no longer
     * need to worry about our crashing */
    private void alertReplicatorsDoneExecutingTask(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.alertDoneExecutingTask(theTask, this);
	    }
	}	
    }

    /** how to actually execute a task localy, this depends on the workflow
     * system that we are working with */
    protected abstract Version executeTaskLocal(Version theTask);

    /** Ask a remote processor to execute a task */
    protected void executeRemoteTask(final Version theTask) {
    	if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! entered executeRemoteTask: theTask.data(): " + theTask.data().getClass().getName());
      	if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! entered executeRemoteTask: theTask.data2(): " + theTask.data2().getClass().getName());
	TaskDefinition td = (TaskDefinition) theTask.data();

	// First we need to find a processor that can handle the task 
	ArrayList al = _poolData.getValidProcessors(td);
	log("number of valid processors:" + al.size());
	log("TaskDefinition of what we are looking for:" + 
	    td);

	// we now make sure that the processor we decide to use is up 
	// and running
	for (int i = 0; i < al.size(); i++) {
	    TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
	    if (psl.survivor.ProcessorMain.debug) System.out.print("IS VALID?");
	    if (tph.valid(this)) {
		if (psl.survivor.ProcessorMain.debug) System.out.println(" YES");
		// everything is ok, start a seperate thread for 
		// remote execution
		final TaskProcessorHandle tph2 = tph;
		final Processor p = this;
		Thread t = new Thread() {
			public void run() {
			    log("PSL! going to invoke tph2.executeTask(...)"); // 2-do: remove
			    tph2.executeTask(theTask, p);
			}
		    };
		t.start();
		return;
	    } else {
		if (psl.survivor.ProcessorMain.debug) System.out.println(" NO");
		// remote processor does not seem to be up
		// let's remove it from our pool
		_poolData.testValidity(tph);
	    }
	}

	// if we get here we could NOT find a processor to execute the task
	// as a result, let's just a wait a bit and hope things get better
	log("no processor to execute task: " + theTask);
	Thread t = new Thread () {
		public void run() {
		    try {
			Thread.sleep(_SLEEP_TIME2);
		    } catch (InterruptedException e) {
			;
		    }
		    queueTask(theTask);
		}
	    };
	t.start();
    }

    /** Given a task that we are about to execute, let's tell the replicators
     * to replicate the task's execution information so that the workflow
     * is survivable.
     */
    private void replicate(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.replicate(theTask, _replicatorQueue, this);
	    }
	}
    }

    /** Queue a task for execution */
    private void queueTask(Version theTask) {
	synchronized(_taskQueue) {
	    _taskQueue.add(theTask);
	}
    }

    /** Start the workflow, this is most probably overwritten and 
     *  implemented differently depending on the underlying workflow system */
    public void startWorkflow(String wfName) {
	log(_processorName);
	log(_tcpPort);
	log(_wfDefPath);
	for (int i = 0; i < _capabilities.size(); i++) {
	    log(_capabilities.get(i).toString());
	}
	// TODO actually start the first task on an appropriate host
    }


    /** shutdown this processor */
    public void shutdown() {
        // TODO: actually do some cleanup here!!!
        System.out.println("Shutting down, received halt command!");
        System.exit(0); // this is UGLY
    }

    /**
     * Tell everyone we know about to shutdown. Does not shutdown
     * this processor (amusingly enough) 
     */
    public void shutdownAll() {
        System.out.println("Shutting down network, received halt command!");
        _poolData.shutdown();
    }


    /**
     * Find a remote processor to execute a task. Also given is a 
     * list of processors we have already used in order to make sure
     * we do go back to an existing processor we tried.
     */
    public void findRemoteProcessor(final Version theTask, ArrayList visited) {
	TaskDefinition td = (TaskDefinition) theTask.data();
	ArrayList al = _poolData.getValidProcessors(td);

	// first we find some processor that can definitely execute the task
	for (int i = 0; i < al.size(); i++) {
	    TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
	    if (tph.valid(this)) {
		final TaskProcessorHandle tph2 = tph;
		final Processor p = this;
		Thread t = new Thread() {
			public void run() {
			    tph2.executeTask(theTask, p);
			}
		    };
		t.start();
		return;
	    } else {
		_poolData.testValidity(tph);
	    }
	}

	// if we cannot find a processor that can definitely execute the task
	// let's send it to another processor that can hopefully find
	// a suitable executing environment.
	Version v = theTask;
	visited.add(_processorName);
	al = _poolData.getProcessors();
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
		boolean flag = false;
		for (int j = 0; j < visited.size(); j++) {
		    if (!tph.getName().equals(visited.get(j))) {
			flag = true;
			break;
		    }
		}
		if (!flag) {
		    tph.findRemoteProcessor(v, visited, this);
		    return;
		}
	    }
	}
	log("very bad, we can't find a processor to run this puppy");
    }

    public boolean ping() { return true; }
    public boolean valid() { return true; }

    protected void log(String s) {
	if (psl.survivor.ProcessorMain.debug) System.err.println(s);
    }
    protected void log(int i) {
	if (psl.survivor.ProcessorMain.debug) System.err.println(i);
    }

    /** Look at the queue of tasks to execute, execute them. */
    public void run() {
	while (true) {
	    synchronized(_taskQueue) {
		while (_taskQueue.size() > 0) {
		    Version v = (Version) _taskQueue.firstElement();
		    _taskQueue.remove(0);
		    executeRemoteTask(v);
		}
	    }
	    try {
		Thread.sleep(_SLEEP_TIME);
	    } catch (InterruptedException e) {
		log(e.toString());
	    }
	}
    }
}

