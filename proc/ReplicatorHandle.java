import java.util.Vector;
public class ReplicatorHandle {
    
    private String _name;
    /**
     * CTOR
     */
    public ReplicatorHandle(String name) {
	_name = name;
    }

    public String getName() { return _name; }

    public void alertExecutingTask(Version v) {
	Version v1 = v.split(null); // no need to move the actual object around
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    public void alertDoneExecutingTask(Version v) {
	Version v1 = v.split(null); // no need to move the actual object around
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    public void replicate(Version v, Vector replicatorQueue) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    /** WE DO NOT NEED TO IMPLEMENT THIS RIGHT NOW, WE JUST USE THE 
	LARGEST UID ALGORITHM */
    public void mediate(Version v) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    // THIS CURRENTLY IS A REVERSE CALL. THIS NEEDS TO BE FIXED 
    // ACTUAL, I DON'T THINK IT'S A REVERSE CALL ANYLONGER 
    public boolean ping() {
	return true;
    }
}
