package psl.survivor.proc;

import java.util.ArrayList;
import java.util.Iterator;

public class TaskProcessorHandle {
    
    private String _name;
    private ArrayList _capabilities;

    /**
     * CTOR
     */
    public TaskProcessorHandle(String name) {
	_capabilities = new ArrayList();
	_name = name;
    }

    public String getName() {
	return _name;
    }

    public void addCapabilities(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	}
    }

    public ArrayList getCapabilities() {
	return _capabilities;
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
}
