package psl.survivor.proc;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Date;
import java.util.Set;
import psl.survivor.util.*;

/**
 * Used to replicator partial workflow states as well as making sure
 * that Processors executing the workflow can survive adverse conditions.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class Replicator implements Runnable {

    
    private final int _TIME_TO_PROCESS = 10 * 1000;
    private int _SLEEP_TIME = 1000;
    private int _executionThreshold = 1000*60;


    /** Workflow states that we know about */
    private VersionCache _versionCache;


    /** Ability to cross reference versions to find the most appropriate */
    private TreeMap _versionLookup;


    /** Tasks currently executing at remote processors that we are
     * monitoring */
    private TreeMap _tasksInProgress;

    
    /** Name of this Replicator */
    private String _name;


    /** Information about other Processors */
    private PoolData _poolData;


    /** The local processor to this Replicator */
    private Processor _processor; 
    
    private psl.survivor.proc.Log _log;

    /** CTOR */
    public Replicator(String name, Processor p) {
	_name = name;
	_processor = p;
	_poolData = p.getPoolData();
	_tasksInProgress = new TreeMap();
	_versionLookup = new TreeMap();
	_log = p.getLog();
    }


    /** Get a handle for this Replicator. The handle can be used to remotely
	communicate with this Replicator */
    public ReplicatorHandle getHandle() {
	return new ReplicatorHandle(_name, _processor.getHostName(), 
				    _processor.getPort());
    }


    /** Since a replicator often shares pool data with a processor this
	allows us to share by having the processor set the replicator's
	pool data */
    public void setPoolData(PoolData p) { _poolData = p; }


    /** Since a replicator often shares a version cache with a processor, 
	this allows us to share by having the processor set the replicator's
	version cache */
    public void setVersionCache(VersionCache vc) { _versionCache = vc; }


    public String getName() { return _name; }


    /** Used to tell this replicator of a task it's supposed to monitor
	on a processor */
    public void alertExecutingTask(Version v) {
	_tasksInProgress.put(v, new Date());
	_log.replicatingTask(v);
    }


    /** Used to tell this replicator that it no longer has to monitor a
	certain task */
    public void alertDoneExecutingTask(Version v) {
	synchronized (_tasksInProgress) {
	    if (_tasksInProgress.containsKey(v)) {
		_log.doneReplicatingTask(v);
		_tasksInProgress.remove(v);
	    } else {
		;
	    }
	}
    }


    /** Add a processor to this replicator's pool data */
    public void addProcessor(TaskProcessorHandle tph) {
	_poolData.addProcessor(tph);
    }


    /** Add a workflow version to be replicated, aong with a list
	of replicators that are also replicating the task */
    public void replicated(Version v, Vector replicatorQueue) {
	_versionCache.addVersion(v);
	_versionLookup.put(v, replicatorQueue);
    }


    /** After we have detected a potential problem with the workflow
	execution, we check to see if there really is a problem if
	there is, we try to make the workflow survive by finding
	alternate processors to re-execute the workflow from the
	latest version. */
    private void survive(Version v) {
	synchronized (_tasksInProgress) {
	    TaskProcessorHandle tph = (TaskProcessorHandle)v.data();
	    if (tph.ping(v.split(null), _processor)) return;
	    _tasksInProgress.remove(v);
	    mediateSurvivor(v);
	}
    }


    /** We are sure that there is a problem with the workflow
        execution. we find an alternate processor to re-execute the
        workflow from the latest version. */
    private void forceSurvive(Version v) {
	synchronized (_tasksInProgress) {
	    TaskProcessorHandle tph = (TaskProcessorHandle)v.data();
	    tph.stopTask(v, _processor);
	    _tasksInProgress.remove(v);
	    mediateSurvivor(v);
	}
    }


    /** Determine which replicator is responsible for surviving the
        workflow. This uses a MAXUID leader election algorithm */
    public void mediateSurvivor(Version v) {
	Vector vec = (Vector) _versionLookup.get(v);
	String maxName = "";
	int maxIndex = -1;
	synchronized (vec) {
	    for (int i = 0; i < vec.size(); i++) {
		ReplicatorHandle rh = (ReplicatorHandle) vec.get(i);
		if ((maxName.compareTo(rh.getName()) < 0) &&
		    (rh.ping(_processor))) {
		    maxName = rh.getName();
		    maxIndex = i;
		}
	    }
	}
	if (maxName.equals(_name)) {
	    executeTask(v);
	}
    }


    /** Given a verison of the workflow that we need to execute, let's
     find the latest version we know about from our Cache, and let's
     then find a processor that can execute that latest version. */
    public void executeTask(Version v) {
	if (_versionCache.getLatestVersion(v).equals(v)) {
	    // get latest version of workflow execution
	    final Version theTask = _versionCache.getLatestVersion(v);
	    TaskDefinition td = (TaskDefinition) theTask.data();

	    // find an appropriate processor
	    ArrayList al = _poolData.getValidProcessors(td);
	    if ((al == null) || (al.size() == 0)) {
		findRemoteProcessor(v);
	    } else {
		for (int i = 0; i < al.size(); i++) {
		    TaskProcessorHandle tph = (TaskProcessorHandle) al.get(i);
		    if (tph.valid(_processor)) {

			// if the processor is up, let's have it execute
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
	    // here we have NO good version of the workflow. This is bad. 
	    // a replicating node should always have a good version of
	    // the workflow.
	    System.err.println("THIS IS VERY BAD, WE DON'T HAVE THE VERSION" +
			       " WE ARE SUPPOSED TO BE REPLICATING");
	}
    }


    /** find a remote processor to execute a particular version of the 
	workflow. Similar to the Processor's implementation */
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


    /** Check all tasks that we are supposed to be monitoring. If the
        processors are down, of it the tasks have timed out, we
        mediate a survivor. */
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

			// if processor is down, let's survive
			if (!tph.ping(v.split(null), _processor)) {
			    _log.processorDown(v);
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

			// if the task has timed out, let's survive
			if ((now.getTime() - _TIME_TO_PROCESS) > d.getTime()) {
			    _log.taskTimeOut(v);
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
