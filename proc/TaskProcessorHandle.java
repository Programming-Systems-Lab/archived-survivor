package psl.survivor.proc;

import java.util.ArrayList;
import java.util.Iterator;

import psl.survivor.net.*;
import psl.survivor.util.*;

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

    public boolean valid() {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
	return true;
    }

    public boolean ping(Version v) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
	return true;
    }

    public void executeTask(Version theTask) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
    }

    public void findRemoteProcessor(Version v, ArrayList al) {
	// NEED TO REMOTELY GET IN TOUCH WITH THE PROCESSOR
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
