package psl.survivor.proc;

import java.util.ArrayList;
import java.util.Iterator;

import java.io.Serializable;

import psl.survivor.net.*;
import psl.survivor.util.*;
import psl.survivor.proc.*;

public class TaskProcessorHandle implements Serializable {
    
    private String _name;
    private String _hostname;
    private int _port;
    private ArrayList _capabilities;

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
    }

    public boolean valid(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setValid(0); // this is bad
	return p.getMessageHandler().sendValid(t);
    }

    public boolean ping(Version v, Processor p) {
	Version v1 = v.split(null); // no need to move the actual object around
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setPing(v1);
	return p.getMessageHandler().sendPing(t);
    }

    public void executeTask(Version theTask, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setExecuteTask(theTask);
	p.getMessageHandler().sendMessage(t);
    }

    public void findRemoteProcessor(Version v, ArrayList al, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setFindRemoteProcessor(v, al);
	p.getMessageHandler().sendMessage(t);
    }

    public void addToCloud(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setAddToCloud(p.getHandle());
	p.getMessageHandler().sendMessage(t);
    }

    public void sendNewHandle(TaskProcessorHandle tph, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setSendNewHandle(tph);
	p.getMessageHandler().sendMessage(t);
    }
    
    public void sendPool(ArrayList al, Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setSendPool(al);
	System.out.println("SEND: " + _name + "@" + _hostname + ":" + _port);
	p.getMessageHandler().sendMessage(t);  
	System.out.println("DONE SEND");
    }

    // local only
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
}




