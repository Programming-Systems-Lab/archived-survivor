package psl.survivor.proc.nrl;

import java.util.Hashtable;
import java.io.Serializable;

public class NRLProcessData implements Serializable {
  public String workflowName = null;
  Object instanceId = null;
  String originTask = null;
  String state = null;
  String nextTaskName = null;
  Hashtable paramTable = null;
  
  public String toString() {
    return "NRLProcessData [w:" + workflowName + ",i:" + instanceId + ",o:" + originTask + ",s:" + state + ",n:" + nextTaskName + ",p:" + paramTable + "]";
  }
}
