package psl.survivor.proc;

import java.util.ArrayList;
import java.io.Serializable;


/**
 * Extra-Definition of a task. Task definitions (task requirements)
 * are matched with Processors' capabilities in order to check if a
 * processor is able to execute a certain task.
 * 
 * Check out the Builder in XML to see how TD objects are constructed
 * from XML.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu) */
public class TaskDefinition implements Serializable {

    private String _name;
    private ArrayList _requirements;

    /**
     * CTOR
     */
    public TaskDefinition(String name) {
	_requirements = new ArrayList();
	_name = name;
    }

    public String getName() {
	return _name;
    }

    /** Add a requirement that an executing processor has to fulfill */
    public void addRequirement(Object o) {
	if (!_requirements.contains(o)) {
	    _requirements.add(o);
	}
    }

    /** get all the requirements that an executing processor has to fulfill */
    public ArrayList getRequirements() {
	return _requirements;
    }

    /** String representation of the task definition's requirements */
    public String toString() {
	String s = _name + ":";
	for (int i = 0; i < _requirements.size(); i++) {
	    s += _requirements.get(i).toString() + "\n";
	}
	return s;
    }
}
