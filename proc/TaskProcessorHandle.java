package psl.survivor.proc;

import java.util.ArrayList;
import java.util.Iterator;

import psl.survivor.net.*;
import psl.survivor.util.*;
import psl.survivor.proc.*;

public class TaskProcessorHandle {
    
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

    public boolean valid(Processor p) {
	VTransportContainer t = new VTransportContainer
	    (_name, _hostname, _port);
	t.setValid();
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

    public boolean match(TaskDefinition td) {
	synchronized (_capabilities) {
	    ArrayList req = td.getRequirements();
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




