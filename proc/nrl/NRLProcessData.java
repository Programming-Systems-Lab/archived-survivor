package psl.survivor.proc.nrl;

import java.util.Hashtable;
import java.util.HashMap;
import java.io.Serializable;

/**
 * This class is used data transfer between different processes
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 */
public class NRLProcessData implements Serializable {
  
  /** The parameter that gets passed */
  public HashMap param = null;

  /** workflow identifier */
  public String workflowName = null;

  /** workflow instance identifier */
  public Object instanceId = null;

  /** identifier for previous task */
  public String originTask = null;

  /** result state of last task execution */
  public String state = null;

  /** identifier for next task */
  public String nextTaskName = null;

  /** container object for other parameters */
  public Hashtable paramTable = null;
  
  public String toString() {
    return "NRLProcessData [w:" + workflowName + ",i:" + instanceId + ",o:" + originTask + ",s:" + state + ",n:" + nextTaskName + ",p:" + paramTable + "]";
  }
}
