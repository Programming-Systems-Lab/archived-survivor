package psl.survivor.proc;

import java.util.Vector;
import java.io.Serializable;

import psl.survivor.util.*;
import psl.survivor.net.*;

/**
 * Handle to a replicator. Though that replicator may be local, this provides
 * for communicating with remove replicators.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class ReplicatorHandle implements Serializable {

    
    private String _name; // name of replicator
    private String _hostname; // hostname of the machine the replicator is on
    private int _port; // port on the machine the replicator is on


    /**
     * CTOR
     */
    public ReplicatorHandle(String name, String hostname, int port) {
	_name = name;
	_hostname = hostname;
	_port = port;
    }


    /** String representation of a replicator */
    public String toString() { return _name + "@" + _hostname + ":" + _port; }

    public String getName() { return _name; }
    public String getHostName() { return _hostname; }
    public int getPort() { return _port; }
    

    /** Alert the refered-to Replicator that the local Processor is
        executing a task. The replicator is now in charge of checking
        that the local Processor is up and running */
    public void alertExecutingTask(Version v, Processor p) {
	Version v1 = v.split(p.getHandle()); // no need to move the 
	                                     // actual task
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAlertExecutingTask(v1);
	p.getMessageHandler().sendMessage(t);
    }


    /** Alert the refered-to Replicator that the local Processor is no
        longer executing a task. The replicator no longer has to check
        that the local Processor is up and running */
    public void alertDoneExecutingTask(Version v, Processor p) {
	Version v1 = v.split(null); // no need to move the actual object around
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAlertDoneExecutingTask(v1);
	p.getMessageHandler().sendMessage(t);
    }


    /** Give the refered-to Replicator a Version of the workflow that
        it may replicate so that we may later restart execution */
    public void replicate(Version v, Vector replicatorQueue, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setReplicate(v, replicatorQueue);
	p.getMessageHandler().sendMessage(t);
    }


    /** WE DO NOT NEED TO IMPLEMENT THIS RIGHT NOW, WE JUST USE THE 
	LARGEST UID ALGORITHM - CHECK OUT REPLICATOR CLASS */
    public void mediate(Version v, Processor p) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }


    /** Check to see if another Processor's replicator is up and running */
    public boolean ping(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setReplicatorPing();
	return p.getMessageHandler().sendReplicatorPing(t);
    }
}

