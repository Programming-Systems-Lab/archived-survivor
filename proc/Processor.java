package psl.survivor.proc;
import java.util.Date;
import java.util.Vector;
import java.util.ArrayList;
import java.net.*;
import psl.survivor.util.*;
import psl.survivor.net.*;
public class Processor implements Runnable {

    protected String _processorName;
    protected int _tcpPort;
    protected String _wfDefPath;
    protected String _rmiName;
    protected String _hostname;

    private VersionCache _versionCache;
    private Vector _taskQueue;
    private Vector _replicatorQueue;
    private PoolData _poolData;
    protected ArrayList _capabilities;
    private WorkflowData _workflowData;
    private MessageHandler _messageHandler;

    private int _SLEEP_TIME = 500;

    /**
     * CTOR
     */
    public Processor(String name, int tcpPort, String rmiName, 
		     String wfDefPath) {
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

    public void addCapability(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	}
    }  

    public ArrayList getCapabilities() {
	return _capabilities;
    }


    public void addProcessor(TaskProcessorHandle tph) {
	_poolData.addProcessor(tph);
    }

    public void addPool(ArrayList al) {
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		addProcessor((TaskProcessorHandle)al.get(i));
	    }
	}
    }

    public void alertNewHandle(TaskProcessorHandle tph) {
	addProcessor(tph);
	ArrayList al = _poolData.getProcessors();
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		TaskProcessorHandle t = (TaskProcessorHandle) al.get(i);
    if (t==null)  {
      System.out.println("t == null");
    } else {
		  t.sendNewHandle(tph, this);
    }
	    }
	    tph.sendPool(al, this);
	}
    }

    public void executeTask(Version theTask) {

	if (_versionCache.contains(theTask)) return;

	_versionCache.addVersion(theTask);

	alertReplicatorsExecutingTask(theTask);

	String taskName = ((TaskDefinition)theTask.data()).getName();
	Version nextTask = executeTaskLocal(theTask);
	// versioning happens here
	nextTask.append(taskName);

	_versionCache.addVersion(nextTask);

	replicate(nextTask);
	queueTask(nextTask);

	alertReplicatorsDoneExecutingTask(theTask);
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

    private Version executeTaskLocal(Version theTask) {
	// NEED TO INTERACT WITH NRL'S CODE HERE
	return null;
    }

    protected void executeRemoteTask(final Version theTask) {
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
	// TODO
	// something like findRemoteProcessor in Replicator
	log("no processor to execute task: " + theTask);
	queueTask(theTask);
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

    public void startWorkflow() {
	log(_processorName);
	log(_tcpPort);
	log(_wfDefPath);
	for (int i = 0; i < _capabilities.size(); i++) {
	    log(_capabilities.get(i).toString());
	}
	// TODO actually start the first task on an appropriate host
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
	System.err.println(s);
    }
    protected void log(int i) {
	System.err.println(i);
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

