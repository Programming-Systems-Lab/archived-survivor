package psl.survivor.proc.nrl;

import psl.worklets.WVM_Host;

import psl.survivor.util.Version;
import psl.survivor.proc.Processor;

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
          Scheduler_Serv scheduler
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
              IScheduler dest = null;
              if (!(theOut.getTask().equals("END"))) {
                /*
                _hostAdapter.executeTransition(namePrefix,
                                               theOut.getTask(),
                                               instanceId,
                                               name,
                                               state,
                                               outBindings);
                */
              } else {
                // WE PROBABLY WANT TO CHANGE THIS TOO SO THAT THE
                // ENDWORKFLOW CALL ALSO GOES THROUGH THE hostAdapter
  
                /* IWFManager man =
                 * (IWFManager)Naming.lookup("rmi://"+theConfig.getManagerHost()+"/"+
                 * namePrefix+"_"+instanceId); man.endWorkflow(null, outBindings);
                 * */
                
                // _hostAdapter.executeEndofWorkflow(instanceId, outBindings);
                /* _hostAdapter.executeTransition(namePrefix,
                 * theConfig.getManagerHost(), instanceId, name, state,
                 * outBindings); */
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
    // NEED TO INTERACT WITH NRL'S CODE HERE
    return null;
  }
  // ENDED: Inherited from psl.survivor.proc.Processor /////////////////////////

}

