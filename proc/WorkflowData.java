package psl.survivor.proc;

import java.util.ArrayList;

import psl.survivor.xml.TaskDefinitionBuilder;

/**
 * General information about the workflow that we need.
 *
 * Currently all the extra information that we need is contained within
 * the task definition xml document. So we load that up and
 * get ready to hand out TaskDefinition objects when required.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class WorkflowData {


    private String _wfDefPath;
    private ArrayList _taskDefinitions;


    /** 
     * CTOR Given the path to workflow data, we build task definition
     * objects out of the task definition xml file.  
     */
    public WorkflowData(String wfDefPath) {
	_wfDefPath = wfDefPath;

	// build the task definitions using a TDBuilder
	TaskDefinitionBuilder tdb = 
	    new TaskDefinitionBuilder(wfDefPath + "/td.xml");
	_taskDefinitions = tdb.getTaskDefinitions();
    }


    /** 
     * Given the name of a task, we return the TaskDefinition for that task.
     * A task definition is a list of name-value pairs that need to be
     * true on the Processor that executes a task.
     */
    public TaskDefinition getTaskDefinition(String taskname) {
	for (int i = 0; i < _taskDefinitions.size(); i++) {
	    if (((TaskDefinition)_taskDefinitions.get(i))
		.getName().equals(taskname)) {
		return (TaskDefinition)_taskDefinitions.get(i);
	    }
	}
	return new TaskDefinition(taskname);
    }
}
