package psl.survivor.proc;
import java.util.ArrayList;


/**
 * Internal representation of remote Processors and Replicators.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class PoolData {
    
    /** Handle to remote processors */
    private ArrayList _processorHandles;

    /** Processor associated with this PoolData */
    private Processor _processor;

    /**
     * CTOR
     */
    public PoolData(Processor p) {
	_processorHandles = new ArrayList();
	_processor = p;
    }

    /**
     * Add a remoteProcessor
     */
    public void addProcessor(TaskProcessorHandle tph) {
	if (!_processorHandles.contains(tph)) {
	    for (int i = 0; i < _processorHandles.size(); i++) {
		if (((TaskProcessorHandle)_processorHandles.get(i)).getName().equals(tph.getName())) return;
	    }
	    if (psl.survivor.ProcessorMain.debug) System.out.println("ADDING a TPH*******************************"); 
	    _processorHandles.add(tph);
	}
    }

    /** 
     * Get all the remote processors that we know about 
     */
    public ArrayList getProcessors() { return _processorHandles; }

    /**
     * Given the definition of a task we need to execute, return
     * an ArrayList of processors that can potentially handle executing
     * that task.
     */
    public ArrayList getValidProcessors(TaskDefinition td) {
	
	ArrayList returnValue = new ArrayList();

	synchronized(_processorHandles) {
	    for (int i = 0; i < _processorHandles.size(); i++) {	
		TaskProcessorHandle tph = (TaskProcessorHandle) 
		    _processorHandles.get(i);
		if (psl.survivor.ProcessorMain.debug) System.out.println("checking out taskHandle:" + tph);
		// see if the processors's capabilities
		// match the TaskDefintion's requirements
		if (tph.match(td)) { 
		    returnValue.add(tph);
		}
	    }
	}
	return returnValue;
    }

    /**
     * Check to see if a tph is valid (aka up and running and responding
     * over the net). If it is not, let's remove it from our knowledge base.
     */
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
    
    /**
     * Send a message to everyone we know about to shutdown.
     */
    public void shutdown() {
      synchronized (_processorHandles) {
        for (int i=0; i<_processorHandles.size(); i++) {
          TaskProcessorHandle tph = (TaskProcessorHandle) _processorHandles.get(i);
          // todo: send the shutdown message!!!
          tph.shutdown(_processor);
        }
      }
    }

    /**
     * String representation of a PoolData.
     * Just a list of the handles that it knows about seperated by newlines.
     */
    public String toString() {
	String s = "";
	synchronized(_processorHandles) {
	    for (int i = 0; i < _processorHandles.size(); i++) {
		s+=_processorHandles.get(i).toString() + "\n";
	    }
	}
	return s;
    }
}
