package psl.survivor.proc.nrl;

import java.util.Hashtable;
import java.io.Serializable;

public class NRLProcessData implements Serializable {
  Object instanceId = null;
  String originTask = null;
  String state = null;
  Hashtable paramTable = null;
}
