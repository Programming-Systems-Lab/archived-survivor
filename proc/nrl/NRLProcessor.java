package psl.survivor.proc.nrl;

import psl.worklets.WVM_Host;

import psl.survivor.util.Version;
import psl.survivor.proc.*;
import psl.survivor.util.*;

import wfruntime.Output;
import wfruntime.IScheduler;
import wfruntime.ConfigInfo;
import wfruntime.RoutingInfo;
import wfruntime.Scheduler_Serv;
import wfruntime.ServiceHost_Serv;
import wfruntime.IRealizationInfo;

import java.util.HashSet;
import java.util.Hashtable;

import java.rmi.RemoteException;

public class NRLProcessor extends Processor {

  private ServiceHost_Serv _serviceHost;
  private Scheduler_Serv _scheduler;
  private final Hashtable _resultDataStorage = new Hashtable();

  // BEGIN: Inherited from psl.survivor.proc.Processor /////////////////////////
  public NRLProcessor(String name, int tcpPort, String rmiName, 
                      String wfDefPath) {
      super(name, tcpPort, rmiName, wfDefPath);
      
      final int rmiPort = WVM_Host.PORT;
      // BEGIN: Inherited from wfruntime.ServiceHost_Serv ///
      try {
	  _serviceHost = new ServiceHost_Serv(rmiPort, wfDefPath) {
		  public void beginScheduler(String name,
					     RoutingInfo theSpecs,
					     HashSet thePerms,
					     Hashtable creates,
					     Hashtable dTypes,
					     Hashtable exceptions,
					     String namePrefix,
					     ConfigInfo theConfig,
					     Hashtable fTypes,
					     IRealizationInfo rinfo)
		      throws RemoteException {
		      Scheduler_Serv scheduler = _scheduler
			  = new Scheduler_Serv(name,
					       theSpecs,
					       thePerms,
					       creates,
					       rmiPort,
					       dTypes,
					       exceptions,
					       namePrefix,
					       theConfig,
					       fTypes,
					       rinfo) {
				  protected void fireNextTask(Output theOut,
							      String state,
							      Hashtable outBindings,
							      Object instanceId) throws Throwable {
				      if (!(theOut.getTask().equals("END"))) {
					  Hashtable paramTable = outBindings;
					  String originTask = this.name; // this task is the next 'originTask'
					  
					  NRLProcessData resultData = new NRLProcessData();
					  resultData.instanceId = instanceId;
					  resultData.originTask = originTask;
					  resultData.state = state;
					  resultData.paramTable = paramTable;
					  resultData.nextTaskName = 
					      theOut.getTask();

                // instanceId.toString() will be used as key!
                _resultDataStorage.put(instanceId.toString(), resultData);

              } else {
                // 2-do: transition to end task
             }
            }   
          }; // ENDED: Scheduler_Serv
  
          // Gskc: copied over from PSLServiceHost_Serv.java: 25 Dec 2001
          // AMIT MOD **
          if (theSpecs.getTaskType().equals("HumanRealization"))
            humanTasks.put(name, scheduler);
          // **
        }
  
      }; // ENDED: ServiceHost_Serv
    } catch (Throwable t) { }
    // ENDED: Inherited from wfruntime.ServiceHost_Serv ///
  }

  protected Version executeTaskLocal(Version theTask) {
    NRLProcessData processData = (NRLProcessData) theTask.data2();

    // need to get the following from 'taskData'
    Object instanceId = processData.instanceId;
    String originTask = processData.originTask;
    String state = processData.state;
    Hashtable paramTable = processData.paramTable;

    // TODO need to fix the instanceId to be unique per task, not workflow
    try {
      _scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (RemoteException re) {
      // left here for backwards compatibility
    }

    NRLProcessData resultData
      = (NRLProcessData) _resultDataStorage.get(instanceId.toString());

    TaskDefinition td = new TaskDefinition(resultData.nextTaskName);
    td.addRequirement(new NameValuePair("tn", resultData.nextTaskName));

    Version result = theTask.split2(resultData, td);

    return result;
  }
  // ENDED: Inherited from psl.survivor.proc.Processor /////////////////////////

}
