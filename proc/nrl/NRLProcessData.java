package psl.survivor.proc.nrl;

import java.util.Hashtable;
import java.io.Serializable;

/**
 * This class is used data transfer between different processes
 */
public class NRLProcessData implements Serializable {
  
  /** workflow identifier */
  public String workflowName = null;

  /** workflow instance identifier */
  Object instanceId = null;

  /** identifier for previous task */
  String originTask = null;

  /** result state of last task execution */
  String state = null;

  /** identifier for next task */
  String nextTaskName = null;

  /** container object for other parameters */
  Hashtable paramTable = null;
  
  public String toString() {
    return "NRLProcessData [w:" + workflowName + ",i:" + instanceId + ",o:" + originTask + ",s:" + state + ",n:" + nextTaskName + ",p:" + paramTable + "]";
  }
}
