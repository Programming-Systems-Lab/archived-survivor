package psl.survivor.net;

import java.io.Serializable;
import java.util.Vector;

import psl.survivor.util.Version;

public class VTransportContainer implements Serializable {
    private Version _v;
    private int _state;
    private Vector _ve;
    private ArrayList _al;
    public VTransportContainer() {
	_state = 0;
    }
    public void setAlertExecutingTask() { _state = 1; }
    public void setAlertDoneExecutingTask() { _state = 2; }
    public void setReplicated(Vector rq) { _state = 3; _ve = rq; }
    public void setMediate() { _state = 4; }
    public void setValid() { _state = 5; }
    public void setPing(Version v) { _state = 6; _v = v; }
    public void setExecuteTask(Version v) { _state = 7; _v = v; }
    public void setFindRemoteProcessor(Version v, ArrayList al) 
    { _v = v; _al = al; }

    public boolean isAlertExecutingTask() { return _state == 1; }
    public boolean isAlertDoneExecutingTask() { return _state == 2; }
    public Vector getReplicated() { return _ve; }
    public boolean isMediate() { return _state == 4; }
    public boolean isValid() { return _state == 5; }
    public boolean isPing() { return _state == 6; }
    public Version getPing() { return _v; }
    public boolean isExecuteTask() { return _state == 7; }
    public Version getExecuteTask() { return _v; }
    public boolean isFindRemoteProcessor() { return _state == 8; }
    public Version getFindRemotePrecessor1() { return _v; }
    public ArrayList getFindRemoteProcessor2() { return _al; }
}
