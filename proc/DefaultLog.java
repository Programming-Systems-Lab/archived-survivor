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
    public void executeTaskLocal(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: executeTaskLocal" + " = " + v);
    }
    public void completedTaskLocal(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: completedTaskLocal" + " = " + v);
    }
    public void stopTaskLocal(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: stopTaskLocal" + " = " + v);
    }
    public void ignoreResultsOfStoppedTask(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: ignoreResultsOfStoppedTask" + " = " + v);
    }

    // task replication
    /*
     * TaskProcessorHandle = v.data();
     * You can get the task processor's name from that.
     */
    public void replicatingTask(Version v, Version finalVer) {
      System.out.println("psl.survivor.proc.DefaultLog :: replicatingTask" + " = " + v);
    }
    public void doneReplicatingTask(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: doneReplicatingTask" + " = " + v);
    }
    public void processorDown(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: processorDown" + " = " + v);
    }
    public void taskTimeOut(Version v) {
      System.out.println("psl.survivor.proc.DefaultLog :: taskTimeOut" + " = " + v);
    }

    // processor capabilities
    public void addedCapability(Object o) {
      System.out.println("psl.survivor.proc.DefaultLog :: addedCapability" + " = " + o);
    }
}
