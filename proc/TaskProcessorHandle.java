package psl.survivor.proc;

import java.util.ArrayList;
import java.util.Iterator;

import java.io.Serializable;

import psl.survivor.net.*;
import psl.survivor.util.*;
import psl.survivor.proc.*;

/**
 * Handle to a TaskProcessor. This is meant to be used to communicate
 * with a Task Processor (which may or may not be remote).
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class TaskProcessorHandle implements Serializable {
    
    
    private String _name; // name of task processor
    private String _hostname; // hostname processor is on
    private int _port; // port of processor
    private ArrayList _capabilities; // the capabilities of the processor
    private ReplicatorHandle _rh = null; // a handle to a local replicator for
                                         // the processor if it has one


    /**
     * CTOR
     */
    public TaskProcessorHandle(TPTransportContainer tptc) {
	_name = tptc.getName();
	_hostname = tptc.getHostName();
	_port = tptc.getPort();
	_capabilities = tptc.getCapabilities();
    }


    /** CTOR */
    public TaskProcessorHandle(String name, String hostname, int port) {
	_name = name;
	_hostname = hostname;
	_port = port;
	_capabilities = new ArrayList();
    }


    /** CTOR */
    public TaskProcessorHandle(Processor p) {
	_name = p.getName();
	_hostname = p.getHostName();
	_port = p.getPort();
	_capabilities = p.getCapabilities();
	if (p.hasMainReplicator()) {
	    addMainReplicator(p.getMainReplicator().getHandle());
	}
    }

    /** Whether the Processor refered to has a local replicator */
    public boolean hasMainReplicator() { return _rh != null; }

    /** Get a handle to the Processor's local replicator if it has one */
    public ReplicatorHandle getReplicatorHandle() { return _rh; }

    /** Set the Processor's local replicator */
    public void addMainReplicator(ReplicatorHandle rh) { _rh = rh; }

    /** Test to see if the refered to Processor is up and running */
    public boolean valid(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setValid(0); // this is bad
	return p.getMessageHandler().sendValid(t);
    }

    /** Test to see if the refered to Processor is up and running and
	executing Version v-task of the workflow */
    public boolean ping(Version v, Processor p) {
	Version v1 = v.split(null); // no need to move the actual object around
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setPing(v1);
	return p.getMessageHandler().sendPing(t);
    }

    /** Tell the refered-to Processor to execute the task in the Version */
    public void executeTask(Version theTask, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! going to invoke t.setExecuteTask(...): " + theTask); // 2-do: remove
	t.setExecuteTask(theTask);
	p.getMessageHandler().sendMessage(t);
    }


    /** Tell the refered-to Processor to stop executing the task in Version */
    public void stopTask(Version theTask, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setStopTask(theTask);
	p.getMessageHandler().sendMessage(t);
    }


    /** Tell the refered-to Processot to shutdown */
    public void shutdown(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setShutdown();
	p.getMessageHandler().sendMessage(t);
    }

    
    /** Given a version-task to execute, and an array list of
	Processors already visited, we ask the refered-to processor to
	try and find a processor to execute the task */
    public void findRemoteProcessor(Version v, ArrayList al, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setFindRemoteProcessor(v, al);
	p.getMessageHandler().sendMessage(t);
    }


    /** Add the local Processor to the cloud of the refered-to processor */
    public void addToCloud(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAddToCloud(p.getHandle());
	p.getMessageHandler().sendMessage(t);
    }

    /** Send the refered-to Processor a handle to another task processor */
    public void sendNewHandle(TaskProcessorHandle tph, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setSendNewHandle(tph);
	p.getMessageHandler().sendMessage(t);
    }
    

    /** Send the refered-to Processor a pool of TPHs */
    public void sendPool(ArrayList al, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setSendPool(al);
	if (psl.survivor.ProcessorMain.debug) System.out.println("SEND: " + _name + "@" + _hostname + ":" + _port);
	p.getMessageHandler().sendMessage(t);  
	if (psl.survivor.ProcessorMain.debug) System.out.println("DONE SEND");
    }

    /** Local call (aka no networking). Checks the refered-to
	Processor's capabilities against a Task's definition. Returns
	true if the refered-to Processor is able to execute that task,
	false otherwise. */
    public boolean match(TaskDefinition td) {
	synchronized (_capabilities) {
	    ArrayList req = td.getRequirements();
	    if (req.size() == 0) {
		return true;
	    }
	    Iterator it = req.iterator();
	    while (it.hasNext()) {
		Object o = it.next();
		boolean flag = false;
		Iterator it2 = _capabilities.iterator();
		while (it2.hasNext()) {
		    if (it2.next().equals(o)) {
			flag = true;
		    }
		}
		if (!flag) {
		    return false;
		}
	    }
	}
	return true;
    }
    
    public String getName() { return _name; }
    public String getHostName() { return _hostname; }
    public int getPort() { return _port; }
    public ArrayList getCapabilities() { return _capabilities; }
    public int getSize() { return _capabilities.size(); }
    public Object getCapability(int i) { return _capabilities.get(i); }

    /** String representation of a TPH */
    public String toString() {
	String s = "{{" + _name + "@" + _hostname + ":" + _port + "}\n";
	synchronized (_capabilities) {
	    for (int i = 0; i < _capabilities.size(); i++) {
		s+=_capabilities.get(i)+"   ";
	    }
	}
	return s + "}";
    }
}




