import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Date;
public class Replicator implements Runnable {
    private int _SLEEP_TIME = 1000;
    private int _executionThreshold = 1000*60;
    private VersionCache _versionCache;
    private HashMap _versionLookup;
    private ArrayList _versions;
    private HashMap _tasksInProgress;
    private String _name;
    private PoolData _poolData;
    
    /** CTOR */
    public Replicator(String name) {
	_name = name;
    }
    public void alertExecutingTask(Version v) {
	_tasksInProgress.put(v, new Date());
    }
    public void alertDoneExecutingTask(Version v) {
	synchronized (_tasksInProgress) {
	    if (_tasksInProgress.containsKey(v)) {
		_tasksInProgress.remove(v);
	    } else {
		System.err.println
		    ("very bad stuff the task was not executing here");
	    }
	}
    }
    public void addProcessor(TaskProcessorHandle tph) {
	_poolData.addProcessor(tph);
    }
    public void replicated(Version v, Vector replicatorQueue) {
	_versionCache.addVersion(v);
	_versionLookup.put(v, replicatorQueue);
    }
    private void survive(Version v) {
	synchronized (_tasksInProgress) {
	    TaskProcessorHandle tph = (TaskProcessorHandle)v.data();
	    if (tph.ping(v.split(null))) return;
	    _tasksInProgress.remove(v);
	    mediateSurvivor(v);
	}
    }
    public void mediateSurvivor(Version v) {
	Vector vec = (Vector) _versionLookup.get(v);
	String maxName = "";
	int maxIndex = -1;
	synchronized (vec) {
	    for (int i = 0; i < vec.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) vec.get(i);
		if ((maxName.compareTo(rh.getName()) < 0) &&
		    (rh.ping())) {
		    maxName = rh.getName();
		    maxIndex = i;
		}
	    }
	}
	if (maxName.equals(_name)) {
	    executeTask(v);
	}
    }
    public void executeTask(Version v) {
	if (_versionCache.getLatestVersion(v).equals(v)) {
	    final Version theTask = _versionCache.getLatestVersion(v);
	    TaskDefinition td = (TaskDefinition) theTask.data();
	    ArrayList al = _poolData.getValidProcessors(td);
	    if ((al == null) || (al.size() == 0)) {
		findRemoteProcessor(v);
	    } else {
		for (int i = 0; i < al.size(); i++) {
		    TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
		    if (tph.valid()) {
			final TaskProcessorHandle tph2 = tph;
			Thread t = new Thread() {
				public void run() {
				    tph2.executeTask(theTask);
				}
			    };
			t.start();
			return;
		    } else {
			_poolData.testValidity(tph);
		    }
		}
	    }
	    findRemoteProcessor(v);
	} else {
	    System.err.println("THIS IS VERY BAD, WE DON'T HAVE THE VERSION" +
			       " WE ARE SUPPOSED TO BE REPLICATING");
	}
    }
    public void findRemoteProcessor(Version v) {
	ArrayList visited = new ArrayList();
	visited.add(_name);
	ArrayList al = _poolData.getProcessors();
	synchronized (al) {
	    for (int i = 0; i < al.size(); i++) {
		TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
		if (!tph.getName().equals(_name)) {
		    tph.findRemoteProcessor(v, visited);
		}
	    }
	}
	System.err.println("very bad, we can't find a remote processor, we" +
			   " cannot run the workflow");
    }
    public void run() {
	while (true) {
	    synchronized (_tasksInProgress) {
		Iterator it = _tasksInProgress.keySet().iterator();
		while (it.hasNext()) {
		    Version v;
		    TaskProcessorHandle tph = (TaskProcessorHandle)
			(v = (Version)it.next()).data();
		    if (!tph.ping(v.split(null))) {
			final Version v1 = v;
			Thread t = new Thread() {
				public void run() {
				    survive(v1);
				}
			    };
			t.start();
		    }
		}
	    }	 
	    try {
		Thread.sleep(_SLEEP_TIME);
	    } catch (InterruptedException e) {
		System.err.println(e.toString());
	    }
	}
    }
}
