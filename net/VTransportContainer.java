package psl.survivor.net;

import java.io.Serializable;
import java.util.Vector;
public class VTransportContainer implements Serializable {
    private Version _v;
    private int _state;
    private Vector _ve;
    public VTransportContainer(Version v) {
	_v = v;
	_state = 0;
    }
    public void setAlertExecutingTask() { _state = 1; }
    public void setAlertDoneExecutingTask() { _state = 2; }
    public void setReplicated(Vector rq) { _state = 3; _ve = rq; }
    public void setMediate() { _state = 4; }

    public boolean isAlertExecutingTask() { return _state == 1; }
    public boolean isAlertDoneExecutingTask() { return _state == 2; }
    public Vector getReplicated() { return _ve; }
    public boolean isMediate() { return _state == 4; }
}
