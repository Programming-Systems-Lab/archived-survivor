package psl.survivor.proc;

import java.util.ArrayList;

public class WorkflowData {

    private String _wfDefPath;
    private ArrayList _taskDefinitions;

    /** CTOR */
    public WorkflowData(String wfDefPath) {
	_wfDefPath = wfDefPath;
	TaskDefinitionBuilder tdb = 
	    new TaskDefinitionBuilder(wfDefPath);
	_taskDefinitions = tdb.getTaskDefinitions();

	for (int i = 0; i < _taskDefinitions.size(); i++) {
	    System.out.println(_taskDefinitions.get(i));
	}
    }
}
