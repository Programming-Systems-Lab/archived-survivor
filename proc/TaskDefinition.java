import java.util.ArrayList;

public class TaskDefinition {

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

    public void addRequirement(Object o) {
	if (!_requirements.contains(o)) {
	    _requirements.add(o);
	}
    }

    public ArrayList getRequirements() {
	return _requirements;
    }

    public String toString() {
	String s = _name + ":";
	for (int i = 0; i < _requirements.size(); i++) {
	    s += _requirements.get(i).toString() + "\n";
	}
	return s;
    }
}
