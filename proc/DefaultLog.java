package psl.survivor.proc;

import psl.survivor.util.Version;

/**
 * Default implementation of general logging interface
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu) 
 */

public class DefaultLog implements Log {

    // task execution
    /*
     * NRLProcessData = v.data2();
     * You can get the taskname from that
     */
    public void executeTaskLocal(Version v) { }
    public void completedTaskLocal(Version v) { }
    public void stopTaskLocal(Version v) { }
    public void ignoreResultsOfStoppedTask(Version v) { }

    // task replication
    /*
     * TaskProcessorHandle = v.data();
     * You can get the task processor's name from that.
     */
    public void replicatingTask(Version v, Version finalVer) { }
    public void doneReplicatingTask(Version v) { }
    public void processorDown(Version v) { }
    public void taskTimeOut(Version v) { }

    // processor capabilities
    public void addedCapability(Object o) { }
}
