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
  private String wfName;
  private String namePrefix;
  
  private final int rmiPort;

  // BEGIN: Inherited from psl.survivor.proc.Processor /////////////////////////
  public NRLProcessor(String name, int tcpPort, String rmiName, 
                      String wfDefPath) {
      super(name, tcpPort, rmiName, wfDefPath);      System.out.println("NRLProcessor: " + name + ":" + tcpPort + "/" + rmiName);

      wfRootDir = wfDefPath;
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
				      System.out.println("fireNextTask ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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
					  System.out.println("FIRING NEXT TASK ***************************************************************************************************************************************");
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
      System.out.println("CORRECT\n\n\n");

    NRLProcessData processData = (NRLProcessData) theTask.data2();

    // need to get the following from 'taskData'
    wfName = processData.workflowName;
    namePrefix = wfName;    Object instanceId = processData.instanceId;
    String originTask = processData.originTask;
    String state = processData.state;
    Hashtable paramTable = processData.paramTable;
    String thisTaskName = processData.nextTaskName; 
    // BIG PROBLEM HERE!! thisTaskName should NOT be null .. and it is!!!

    // TODO need to fix the instanceId to be unique per task, not workflow
    try {
      resetTask(thisTaskName); // copied from WFLoader
      log("PSL! returned from resetTask, now calling _scheduler.transition(...): instanceId: " + instanceId + ", originTask: " + originTask + ", state: " + state + ", paramTable: " + paramTable); // 2-do: remove
  
      _scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (RemoteException re) {
      // left here for backwards compatibility
      // possibly thrown by: _scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (Throwable t) {
      // left here for backwards compatibility
      // possibly thrown by: resetTask(thisTaskName);
    }
    NRLProcessData resultData = null;
    while (resultData == null) {
	resultData
	    = (NRLProcessData) _resultDataStorage.get(instanceId.toString());
    }
    _resultDataStorage.remove(instanceId.toString());

    if (resultData == null) {
	System.out.println("resultData is null");
    } else {
	System.out.println("resultData is NOT null");
    }

    /*    TaskDefinition td = new TaskDefinition(resultData.nextTaskName);
	  td.addRequirement(new NameValuePair(resultData.nextTaskName, "true"));*/
    TaskDefinition td = _workflowData.getTaskDefinition(resultData.nextTaskName);

    Version result = theTask.split2(td, resultData); // ya had these reversed :)

    if (result == null) System.out.println("|||||||||||\\\\\\ bad");

    return result;
  }

    public void	startWorkflow(String wfName_iKey) {
    log(_processorName);
    log(_tcpPort);
    log(_wfDefPath);
    log(wfName_iKey);
    for	(int i = 0;	i	<	_capabilities.size();	i++) {
      log(_capabilities.get(i).toString());
    }
    /**
     * TODO: need to do what WFManager does
     * ConfigInfo, StartTask, StartHost, InstanceKey, InitialBindings(empty), StartNode->transition();
     * Notes: need different behaviour for top-level WFManager, and 
     * nested WFManagers ... mainly to do with bindings, schedulers, etc
    */
    int separator = wfName_iKey.indexOf('-');
    wfName = wfName_iKey.substring(0, separator);
    String _iKey = wfName_iKey.substring(separator+1);

    // BEGIN: Inherited from wfruntime.WFManager_Serv ///
    ConfigInfo _config = null;
    try {
      _config = SpecParser.parseConfig(new File(_wfDefPath,"workflows"+File.separatorChar+wfName+File.separatorChar+"config"));
    } catch (Throwable t) {
      System.err.println("Throwable in startWorkflow: parseConfig");
      t.printStackTrace();
    }
    String _startTask = _config.getStartTask();
    // StartHost: no need to get _config.getStartHost() here ... 
    // InstanceKey: same as npd.instanceId below
    // InitialBindings(empty): same as npd.paramTable below
    // ENDED: Inherited from wfruntime.WFManager_Serv ///

    // TODO	actually start the first task	on an	appropriate	host
    TaskDefinition td	= _workflowData.getTaskDefinition(_startTask);

    NRLProcessData npd = new NRLProcessData();
    
    npd.workflowName = wfName;
    npd.instanceId = new Integer(_iKey);

    // TODO should that be "start"
    npd.originTask = "START";
    npd.state	=	"Success";
    npd.nextTaskName = _startTask;
    npd.paramTable = new Hashtable();

    Version	v	=	new	Version(td);
    v.setData2(npd);
    log("PSL!	going	to invoke	executeRemoteTask(...)");	// 2-do: remove
    executeRemoteTask(v);
	}

// ENDED: Inherited from psl.survivor.proc.Processor /////////////////////////

  // BEGIN: Copied from wfruntime.psl.PSLWFLoader //////////////////////////////
  /**
   * resetTask copied over from WFLoader ... 
   * this will load NRL-defined spec files from the local filesystem
   * and internally call beginScheduler(...) on the local scheduler
  */
  public void resetTask(String taskName) throws Throwable {
System.out.println("4: GOT AS FAR AS HERE!!!, wfName: " + wfName + ", taskName: " + taskName);
    final char sc = File.separatorChar;
    File              subDir      = new File(wfRootDir,"workflows"+sc+wfName+sc+taskName);
    System.err.println("1");
    System.err.println("2");    RoutingInfo       theRouting  = SpecParser.parseRouting(new File(subDir,"routing"));
    System.err.println("3");    HashSet           thePerms    = SpecParser.parseDataPerms(new File(subDir,"dataperm"));
    System.err.println("4");    Hashtable         creates     = SpecParser.parseCreates(new File(subDir,"creates"));
    System.err.println("5");Hashtable         dTypes      = SpecParser.parseDataTypes(new File(subDir,"datatypes"));
    System.err.println("6");    Hashtable         exceptions  = SpecParser.parseExceptions(new File(subDir,"exceptions"));
    System.err.println("7");Hashtable         fTypes      = SpecParser.parseFieldTypes(new File(subDir,"fieldtypes"));
    System.err.println("8");    IRealizationInfo  rinfo       = SpecParser.parseRealization(new File(subDir,"realization"));
    System.err.println("9");
    System.err.println("10");    IServiceHost theHost = (IServiceHost) Naming.lookup("rmi://localhost:" + rmiPort + "/serviceHost");
    System.err.println("11");
    System.err.println("12");    ConfigInfo theConfig = SpecParser.parseConfig(new File(wfRootDir, "workflows" + sc + wfName + sc + "config"));
    System.err.println("13");
    if (theRouting.getTaskType().equals("NonTransactionalTaskRealization") ||
      theRouting.getTaskType().equals("SynchronizationTaskIn") ||
      theRouting.getTaskType().equals("SynchronizationTaskOut")) {
      String[] files = subDir.list(new FilenameFilter() {
        public boolean accept(File file, String name) {
          return name.endsWith(".class");
        }
      });
      
log("PSL! inside resetTask: going to invoke theHost.putRealizationInClasspath(...): "); // 2-do: remove
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
log("PSL! inside resetTask: going to beginScheduler(...): namePrefix: " + namePrefix); // 2-do: remove
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
