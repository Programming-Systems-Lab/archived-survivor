package psl.survivor.proc;

import java.util.Vector;

import psl.survivor.util.*;
import psl.survivor.net.*;

public class ReplicatorHandle {
    
    private String _name; 
    private String _hostname;
    private int _port;
    /**
     * CTOR
     */
    public ReplicatorHandle(String name, String hostname, int port) {
	_name = name;
	_hostname = hostname;
	_port = port;
    }

    public String getName() { return _name; }
    public String getHostName() { return _hostname; }
    public int getPort() { return _port; }
    
    public void alertExecutingTask(Version v, Processor p) {
	Version v1 = v.split(null); // no need to move the actual object around
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAlertExecutingTask(v1);
	p.getMessageHandler().sendMessage(t);
    }

    public void alertDoneExecutingTask(Version v, Processor p) {
	Version v1 = v.split(null); // no need to move the actual object around
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAlertDoneExecutingTask(v1);
	p.getMessageHandler().sendMessage(t);
    }

    public void replicate(Version v, Vector replicatorQueue, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setReplicate(v, replicatorQueue);
	p.getMessageHandler().sendMessage(t);
    }

    /** WE DO NOT NEED TO IMPLEMENT THIS RIGHT NOW, WE JUST USE THE 
	LARGEST UID ALGORITHM */
    public void mediate(Version v, Processor p) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    public boolean ping(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setReplicatorPing();
	return p.getMessageHandler().sendReplicatorPing(t);
    }
}

