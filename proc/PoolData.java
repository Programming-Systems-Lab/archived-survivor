package psl.survivor.proc;
import java.util.ArrayList;

public class PoolData {
    
    private ArrayList _processorHandles;
    private Processor _processor; // TODO initialize this

    /**
     * CTOR
     */
    public PoolData() {
	_processorHandles = new ArrayList();
    }

    public void addProcessor(TaskProcessorHandle tph) {
	if (!_processorHandles.contains(tph)) {
	    _processorHandles.add(tph);
	}
    }

    public ArrayList getProcessors() { return _processorHandles; }

    public ArrayList getValidProcessors(TaskDefinition td) {
	
	ArrayList returnValue = new ArrayList();

	synchronized(_processorHandles) {
	    for (int i = 0; i < _processorHandles.size(); i++) {
		TaskProcessorHandle tph = (TaskProcessorHandle) 
		    _processorHandles.get(i);
		if (tph.match(td)) {
		    returnValue.add(tph);
		}
	    }
	}
	return returnValue;
    }

    public void testValidity(TaskProcessorHandle tph) {
	try {
	    if (tph.valid(_processor)) {
		return;
	    }
	} catch (Exception e) {
	    ;
	}
	synchronized(_processorHandles) {
	    _processorHandles.remove(tph);
	}
    }
}
