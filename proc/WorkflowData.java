package psl.survivor.proc;

import java.util.ArrayList;

import psl.survivor.xml.TaskDefinitionBuilder;

public class WorkflowData {

    private String _wfDefPath;
    private ArrayList _taskDefinitions;

    /** CTOR */
    public WorkflowData(String wfDefPath) {
	_wfDefPath = wfDefPath;
	TaskDefinitionBuilder tdb = 
	    new TaskDefinitionBuilder(wfDefPath + "/td.xml");
	_taskDefinitions = tdb.getTaskDefinitions();

	for (int i = 0; i < _taskDefinitions.size(); i++) {
	    System.out.println(_taskDefinitions.get(i));
	}
    }

    //NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO

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
