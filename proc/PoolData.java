package psl.survivor.proc;
import java.util.ArrayList;

public class PoolData {
    
    private ArrayList _processorHandles;
    private Processor _processor;

    /**
     * CTOR
     */
    public PoolData(Processor p) {
	_processorHandles = new ArrayList();
	_processor = p;
    }

    public void addProcessor(TaskProcessorHandle tph) {
	System.out.println("ADDING a TPH*******************************");
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
		System.out.println("checking out taskHandle:" + tph);
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
