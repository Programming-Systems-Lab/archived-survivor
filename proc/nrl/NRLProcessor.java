package psl.survivor.proc.nrl;

import psl.worklets.WVM_Host;

import psl.survivor.proc.Processor;
import psl.survivor.proc.TaskDefinition;

import psl.survivor.util.Version;
import psl.survivor.util.NameValuePair;

import wfruntime.Output;
import wfruntime.IScheduler;
import wfruntime.SpecParser;
import wfruntime.ConfigInfo;
import wfruntime.RoutingInfo;
import wfruntime.IServiceHost;
import wfruntime.Scheduler_Serv;
import wfruntime.ServiceHost_Serv;
import wfruntime.IRealizationInfo;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Hashtable;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class NRLProcessor extends Processor {

  private ServiceHost_Serv _serviceHost;
  private Scheduler_Serv _scheduler;
  private final Hashtable _resultDataStorage = new Hashtable();

  private final String wfRootDir;
  private final String wfName;
  private final String namePrefix;
  
  private final int rmiPort;

  // BEGIN: Inherited from psl.survivor.proc.Processor /////////////////////////
  public NRLProcessor(String name, int tcpPort, String rmiName, 
                      String wfDefPath) {
      super(name, tcpPort, rmiName, wfDefPath);

      wfRootDir = wfDefPath;
      wfName = "A_WORKFLOW"; // todo: need to get real wfName
      namePrefix = wfName; // todo: what is this really supposed to be?
      
      rmiPort = WVM_Host.PORT;
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
    String thisTaskName = processData.nextTaskName;

    // TODO need to fix the instanceId to be unique per task, not workflow
    try {
      resetTask(thisTaskName); // copied from WFLoader
      // this will internally call beginScheduler(...)
  
      _scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (RemoteException re) {
      // left here for backwards compatibility
      // possibly thrown by: _scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (Throwable t) {
      // left here for backwards compatibility
      // possibly thrown by: resetTask(thisTaskName);
    }

    NRLProcessData resultData
      = (NRLProcessData) _resultDataStorage.get(instanceId.toString());
    _resultDataStorage.remove(instanceId.toString());

    TaskDefinition td = new TaskDefinition(resultData.nextTaskName);
    td.addRequirement(new NameValuePair(resultData.nextTaskName, "true"));

    Version result = theTask.split2(resultData, td);

    return result;
  }
  // ENDED: Inherited from psl.survivor.proc.Processor /////////////////////////

  // BEGIN: Copied from wfruntime.psl.PSLWFLoader //////////////////////////////
  /**
   * resetTask copied over from WFLoader ... 
   * this will load NRL-defined spec files from the local filesystem
   * and internally call beginScheduler(...) on the local scheduler
  */
  public void resetTask(String taskName) throws Throwable {
    final char sc = File.separatorChar;
    File              subDir      = new File(wfRootDir,"workflows"+sc+wfName+sc+taskName);
    RoutingInfo       theRouting  = SpecParser.parseRouting(new File(subDir,"routing"));
    HashSet           thePerms    = SpecParser.parseDataPerms(new File(subDir,"dataperm"));
    Hashtable         creates     = SpecParser.parseCreates(new File(subDir,"creates"));
    Hashtable         dTypes      = SpecParser.parseDataTypes(new File(subDir,"datatypes"));
    Hashtable         exceptions  = SpecParser.parseExceptions(new File(subDir,"exceptions"));
    Hashtable         fTypes      = SpecParser.parseFieldTypes(new File(subDir,"fieldtypes"));
    IRealizationInfo  rinfo       = SpecParser.parseRealization(new File(subDir,"realization"));

    IServiceHost theHost = (IServiceHost) Naming.lookup("rmi://localhost:" + rmiPort + "/serviceHost");
    ConfigInfo theConfig = SpecParser.parseConfig(new File(wfRootDir, "workflows" + sc + wfName + sc + "config"));

    if (theRouting.getTaskType().equals("NonTransactionalTaskRealization") ||
      theRouting.getTaskType().equals("SynchronizationTaskIn") ||
      theRouting.getTaskType().equals("SynchronizationTaskOut")) {
      String[] files = subDir.list(new FilenameFilter() {
        public boolean accept(File file, String name) {
          return name.endsWith(".class");
        }
      });
      
      for (int i=0; i<files.length; i++)
        theHost.putRealizationInClasspath(wfName,taskName,files[i], readClassFile(new File(subDir,files[i])));
    }
    
    try {
      if (theRouting.getTaskType().equals("SynchronizationTaskIn")) {
        /*
         * // Not implemented: 27 Dec 2001
        LOG.println("starting syncin");
        theHost.beginSyncInManager(taskName,theRouting,thePerms,creates,dTypes,namePrefix,theConfig,fTypes);
        */
      } else {
        /*
         * // Not implemented: 27 Dec 2001
        if (theRouting.getTaskType().equals("NonTransactionalNetwork")) {
          PSLWFLoader newLoader = new PSLWFLoader(wfRootDir.getCanonicalPath(),
                                                  theRouting.getNestedWFName(),
                                                  namePrefix + "." + taskName,
                                                  regPort,
                                                  taskHostName);
          newLoader.startSchedulers();
          WFTaskRealizationInfo wfrealinfo = (WFTaskRealizationInfo) rinfo;
          wfrealinfo.setStartHost(newLoader.getConfig().getHostForTask(wfrealinfo.getStartTask()));
        }
        */
        theHost.beginScheduler(taskName,theRouting,thePerms,creates,dTypes,exceptions,namePrefix,theConfig,fTypes,rinfo);
     }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    
    // send data classFiles
    Object[] createParams = creates.keySet().toArray();
    String className;
    FileInputStream fileIn;
    HashSet classesToLoad = new HashSet();
    
    for (int p=0;p<createParams.length;p++) {
      className = (String) creates.get(createParams[p]);
      classesToLoad.add(className);
    }
    
    Object[] useParams = dTypes.keySet().toArray();
    for (int p=0;p<useParams.length;p++) {
      className = (String) dTypes.get(useParams[p]);
      classesToLoad.add(className.substring(0,className.lastIndexOf(".")+1)+"I"+className.substring(className.lastIndexOf(".")+1));
      classesToLoad.add(className+"_Stub");
    }
    
    Object[] exceps = exceptions.keySet().toArray();
    for (int p=0;p<exceps.length;p++) {
      classesToLoad.add(exceps[p]);
    }
    // System.out.println("GET RID OF ME! classesToLoad.size(): " + classesToLoad.size());

    Iterator classIter = classesToLoad.iterator();
    while (classIter.hasNext()) {
      className = (String)classIter.next();
      // theHost.putDataClassInClasspath(className,readClassFile(new File(wfRootDir.getCanonicalPath()+File.separatorChar+"data"+File.separatorChar+className.replace('.','\\')+".class")));
      // System.out.println("PSL! change");        theHost.putDataClassInClasspath(className,readClassFile(new File(wfRootDir.getCanonicalPath()+File.separatorChar+"data"+File.separatorChar+className.replace('.',File.separatorChar)+".class")));
      // todo: why aren't we sending the class files over?
      
    }
    // new for loop/ send new configs
  }

  private static byte[] readClassFile(File theFile) throws Throwable {
    FileInputStream in = new FileInputStream(theFile);
    byte[] returnObj = new byte[(int)theFile.length()];
    in.read(returnObj);
    in.close();
    return returnObj;
  }

  // ENDED: Copied from wfruntime.psl.PSLWFLoader //////////////////////////////
 
}
