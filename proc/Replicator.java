package psl.survivor.proc;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Date;
import java.util.Set;
import psl.survivor.util.*;
public class Replicator implements Runnable {
    
    private final int _TIME_TO_PROCESS = 10 * 1000;

    private int _SLEEP_TIME = 1000;
    private int _executionThreshold = 1000*60;
    private VersionCache _versionCache;
    private TreeMap _versionLookup;
    private ArrayList _versions;
    private TreeMap _tasksInProgress;
    private String _name;
    private PoolData _poolData;
    private Processor _processor; 
    
    /** CTOR */
    public Replicator(String name, Processor p) {
	_name = name;
	_processor = p;
	_poolData = p.getPoolData();
	_tasksInProgress = new TreeMap();
	_versionLookup = new TreeMap();
	_versions = new ArrayList();
    }
    public ReplicatorHandle getHandle() {
	return new ReplicatorHandle(_name, _processor.getHostName(), 
				    _processor.getPort());
    }
    public void setPoolData(PoolData p) { _poolData = p; }
    public void setVersionCache(VersionCache vc) { _versionCache = vc; }
    public String getName() { return _name; }
    public void alertExecutingTask(Version v) {
	System.out.println("***********************(((((((((((())))))))))))) Alert we are executing as task somewhere else");
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
	    if (tph.ping(v.split(null), _processor)) return;
	    _tasksInProgress.remove(v);
	    mediateSurvivor(v);
	}
    }
    private void forceSurvive(Version v) {
	synchronized (_tasksInProgress) {
	    TaskProcessorHandle tph = (TaskProcessorHandle)v.data();
	    tph.stopTask(v, _processor);
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
		System.out.println("DOING THE PING THING: " + rh);
		if ((maxName.compareTo(rh.getName()) < 0) &&
		    (rh.ping(_processor))) {
		    maxName = rh.getName();
		    maxIndex = i;
		}
	    }
	}
	if (maxName.equals(_name)) {
	    System.out.println("ABOUT TO EXECUTE");
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
		    if (tph.valid(_processor)) {
			final TaskProcessorHandle tph2 = tph;
			Thread t = new Thread() {
				public void run() {
				    tph2.executeTask(theTask, _processor);
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
		    tph.findRemoteProcessor(v, visited, _processor);
		}
	    }
	}
	System.err.println("very bad, we can't find a remote processor, we" +
			   " cannot run the workflow");
    }
    public boolean ping() {
	return true;
    }
    public void run() {
	while (true) {
	    synchronized (_tasksInProgress) {
		Set s = _tasksInProgress.keySet();
		if (s != null) {
		    Iterator it = _tasksInProgress.keySet().iterator();
		    while (it.hasNext()) {
			Version v;
			TaskProcessorHandle tph = (TaskProcessorHandle)
			    (v = (Version)it.next()).data();
			if (tph == null) { System.out.println("TPH IS NULL"); }
			if (v.split(null) == null) { System.out.println("v.split is NULL");}
			if (!tph.ping(v.split(null), _processor)) {
			    final Version v1 = v;
			    Thread t = new Thread() {
				    public void run() {
					survive(v1);
				    }
				};
			    t.start();
			}
			Date d = (Date) _tasksInProgress.get(v);
			Date now = new Date();
			if ((now.getTime() - _TIME_TO_PROCESS) > d.getTime()) {
			    final Version v1 = v;
			    Thread t = new Thread() {
				    public void run() {
					forceSurvive(v1);
				    }
				};
			    t.start();
			}
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
