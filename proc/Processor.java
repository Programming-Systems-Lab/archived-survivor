package psl.survivor.proc;

import java.util.Set;
import java.util.Date;
import java.util.Vector;
import java.util.HashSet;
import java.util.ArrayList;

import java.net.InetAddress;
import java.net.UnknownHostException;

import psl.survivor.net.*;
import psl.survivor.util.*;

public abstract class Processor implements Runnable {

    protected String _processorName;
    protected int _tcpPort;
    protected String _wfDefPath;
    protected String _rmiName;
    protected String _hostname;

    private Set _stoppedTasks;
    private VersionCache _versionCache;
    private Vector _taskQueue;
    private Vector _replicatorQueue;
    private PoolData _poolData;
    protected ArrayList _capabilities;
    protected WorkflowData _workflowData;
    private MessageHandler _messageHandler;
    protected Replicator _replicator = null;
    protected boolean _hasReplicator = false;

    private int _SLEEP_TIME = 500;
    private int _SLEEP_TIME2 = 1000;

    /**
     * CTOR
     */
    public Processor(String name, int tcpPort, String rmiName, 
		     String wfDefPath) {
	_stoppedTasks = new HashSet();
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

    public TaskProcessorHandle getHandle() {
	return new TaskProcessorHandle(this);
    }

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

    public boolean hasMainReplicator() { return _hasReplicator; }
    public Replicator getMainReplicator() { return _replicator; }

    public void addReplicator(ReplicatorHandle rh) {
	synchronized(_replicatorQueue) {
	    _replicatorQueue.add(rh);
	}
    }

    public void addCapability(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	}
    }

    public ArrayList getCapabilities() {
	return _capabilities;
    }

    public Vector getReplicators() { return _replicatorQueue; }


    public void addProcessor(TaskProcessorHandle tph) {
	_poolData.addProcessor(tph);
	if (tph.hasMainReplicator()) {
	    System.out.println("WE ARE ADDING A REPLICATOR HERE: " + tph);
	    ReplicatorHandle rh = tph.getReplicatorHandle();
	    if (!rh.getName().equals(_replicator.getName())) {
		addReplicator(rh);
	    }
	}
    }

    public void addPool(ArrayList al) {
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		addProcessor((TaskProcessorHandle)al.get(i));
	    }
	}
    }

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
		    t.sendNewHandle(tph, this);
		}
	    }
	    addProcessor(tph);
	    if (psl.survivor.ProcessorMain.debug) System.out.println("tph.sendPool");
	    tph.sendPool(al, this);
	}
    }

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

        System.out.println(" - - - - - - - - - - - PSL! entered executeTask, parameter version>  " + theTask);
	// if (_versionCache.contains(theTask)) return;

	_versionCache.addVersion(theTask);
	replicate(theTask);

	alertReplicatorsExecutingTask(theTask);

	String taskName = ((TaskDefinition)theTask.data()).getName();
	Version nextTask = executeTaskLocal(theTask);
	// versioning happens here
	// TODO remove nonsense hereunder


	if (_stoppedTasks.contains(theTask)) {
	    // this task was supposed to be stopped .. can ignore result
	    _stoppedTasks.remove(theTask);

	} else if (nextTask == null) {
	    System.out.println("WORKFLOW IS DONE");
	    alertReplicatorsDoneExecutingTask(theTask);

	} else {
	    nextTask.append(taskName);
	    // todo: 16-Feb maybe uncomment this?? _versionCache.addVersion(nextTask);
	    queueTask(nextTask);
	    System.out.println("WE DO NOT GET HERE");
	    alertReplicatorsDoneExecutingTask(theTask);
	}
    }

    public void stopTask(Version v) {
	System.err.println(" ##############################################################################################################################################################\nSTOP TASK WOULD BE HAPPENING NOW\n"+v+"\n##############################################################################################################################################################");
	// WE NEED TO FIGURE OUT HOW TO DO THIS PROPERLY
	// RIGHT NOW, CAN WE STOP A RUNNING TASK
	_stoppedTasks.add(v);
    }

    private void alertReplicatorsExecutingTask(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.alertExecutingTask(theTask, this);
	    }
	}	
    }

    private void alertReplicatorsDoneExecutingTask(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.alertDoneExecutingTask(theTask, this);
	    }
	}	
    }

    protected abstract Version executeTaskLocal(Version theTask);

    protected void executeRemoteTask(final Version theTask) {
    	if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! entered executeRemoteTask: theTask.data(): " + theTask.data().getClass().getName());
      	if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! entered executeRemoteTask: theTask.data2(): " + theTask.data2().getClass().getName());
	TaskDefinition td = (TaskDefinition) theTask.data();
	ArrayList al = _poolData.getValidProcessors(td);
	log("number of valid processors:" + al.size());
	log("TaskDefinition of what we are looking for:" + 
	    td);
	for (int i = 0; i < al.size(); i++) {
	    TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
	    if (psl.survivor.ProcessorMain.debug) System.out.print("IS VALID?");
	    if (tph.valid(this)) {
		if (psl.survivor.ProcessorMain.debug) System.out.println(" YES");
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
		_poolData.testValidity(tph);
	    }
	}
	// TODO
	// something like findRemoteProcessor in Replicator
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
	// TODO
	// uncomment the following line in the long term
	//	queueTask(theTask);
    }

    private void replicate(Version theTask) {
	synchronized(_replicatorQueue) {
	    for (int i = 0; i < _replicatorQueue.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) 
		    _replicatorQueue.get(i);
		rh.replicate(theTask, _replicatorQueue, this);
	    }
	}
    }

    private void queueTask(Version theTask) {
	synchronized(_taskQueue) {
	    _taskQueue.add(theTask);
	}
    }

    public void startWorkflow(String wfName) {
	log(_processorName);
	log(_tcpPort);
	log(_wfDefPath);
	for (int i = 0; i < _capabilities.size(); i++) {
	    log(_capabilities.get(i).toString());
	}
	// TODO actually start the first task on an appropriate host
    }

    public void shutdown() {
        // TODO: actually do some cleanup here!!!
        System.out.println("Shutting down, received halt command!");
        System.exit(0);
    }

    public void findRemoteProcessor(final Version theTask, ArrayList visited) {
	TaskDefinition td = (TaskDefinition) theTask.data();
	ArrayList al = _poolData.getValidProcessors(td);
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

