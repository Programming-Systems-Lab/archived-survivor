package psl.survivor.proc.nrl;

import psl.worklets.WVM_Host;

import psl.survivor.proc.Log;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Hashtable;

import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 *
 * NRL's taskProcessor-specific implementation of
 * psl.survivor.Processor.  This class translates an
 *
 * @see(executeTaskLocal) method invocation, and provides the mapping
 * to NRL's transition(..) function that actually executes the task
 * 
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 *  */
public class NRLProcessor extends Processor {

  /** to transfer results of task execution */
  private final Hashtable _resultDataStorage = new Hashtable();

  /** NRL-specific: filesystem reference to location of execution specifiles */
  private final String wfDefPath;

  /** alias for wfDefPath */
  private final String wfRootDir;

  /** port that the local rmiRegistry for the WVM is running */
  private final int rmiPort;
  
  /** singleton NRL serviceHost object that will create schedulers for each task */
  private NRLServiceHost _serviceHost = null;

  // BEGIN: Inherited from psl.survivor.proc.Processor /////////////////////////

  private static HashMap _latestParam;

  /** Initialise and setup processor */
  public NRLProcessor(String name, int tcpPort, String rmiName, 
                      String wfDefPath, Log l) {
    super(name, tcpPort, rmiName, wfDefPath, l);
    if (psl.survivor.ProcessorMain.debug) System.out.println("NRLProcessor: " + name + ":" + tcpPort + "/" + rmiName);

    wfRootDir = this.wfDefPath = wfDefPath;
    rmiPort = WVM_Host.PORT;

  }

  /**
   * abstract function inherited from superclass
   *  - extracts NRL-style input data for the local task, 
   *  - passes control to the NRL serviceHost, scheduler
   *  - waits for the data resulting from the execution, 
   *    and returns this
   */
  protected Version executeTaskLocal(Version theTask) {
    if (psl.survivor.ProcessorMain.debug) System.out.println("CORRECT\n\n\n");

    _log.executeTaskLocal(theTask);


    // TODO need to fix the instanceId to be unique per task, not workflow
    final NRLProcessData processData = (NRLProcessData) theTask.data2();

    // TODO Modify nrl's code to not use RMI, and get rid of this
    
    _latestParam = new HashMap(); // TODO threads will cause problems here
    _latestParam.putAll(processData.param);

    final Object instanceId = processData.instanceId;
    try {
      // copied from WFLoader
      // 2/20/2002: converted from resetTask(thisTaskName) into resetTask(processData)
      resetTask(processData); // this eventually gets the task to get executed
      
      // 2/20/2002, Gskc: the call to transition has been moved into resetTask
    } catch (Throwable t) {
      // left here for backwards compatibility
      // possibly thrown by: resetTask(thisTaskName);
      // actually, 
      t.printStackTrace();
    }
    
    // todo: probably don't need the following loop
    NRLProcessData resultData = null;
    while (resultData == null) {
      resultData
        = (NRLProcessData) _resultDataStorage.get(instanceId.toString());
      // retrieve the output data of the task execution
      try {
	  Thread.sleep(500);
      } catch (Exception e) {
	  ;
      }
    }
    _resultDataStorage.remove(instanceId.toString());

    if (resultData.workflowName.equals("END")) return null;
    TaskDefinition td = _workflowData.getTaskDefinition(resultData.nextTaskName);
    Version result = theTask.split2(td, resultData);
    // System.out.println(" - - - - - - - - - - - PSL! executeTaskLocal returning version> " + result);

    resultData.param = _latestParam;

    return result;
  }

  /**
   * Initialising directive to start the specified workflow
   * @param wfName_iKey: string composed of the NRL-specific
   *  workflowName and instanceIdentifier
   */
  public void startWorkflow(String wfName_iKey) {
    log(_processorName);
    log(_tcpPort);
    log(_wfDefPath);
    log(wfName_iKey);
    for	(int i=0; i<_capabilities.size(); i++) {
      log(_capabilities.get(i).toString());
    }
    
    /**
    * This does what the WFManager does
    * ConfigInfo, StartTask, StartHost, InstanceKey, InitialBindings(empty), StartNode->transition();
    * Notes: need different behaviour for top-level WFManager, and 
    * nested WFManagers ... mainly to do with bindings, schedulers, etc
    */
    int separator = wfName_iKey.indexOf('-');
    String wfName = wfName_iKey.substring(0, separator);
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
    npd.param = new HashMap();

    // TODO should that be "start"
    npd.originTask = "START";
    npd.state	=	"Success";
    npd.nextTaskName = _startTask;
    npd.paramTable = new Hashtable();

    // construct the input data for the first task
    Version	v	=	new	Version(td);
    v.setData2(npd);
    // log("PSL!	going	to invoke	executeRemoteTask(...)");
    executeRemoteTask(v);
  }

  // ENDED: Inherited from psl.survivor.proc.Processor /////////////////////////

  // BEGIN: Copied from wfruntime.psl.PSLWFLoader //////////////////////////////
  /**
   * resetTask copied over from WFLoader ... 
   * this will load NRL-defined spec files from the local filesystem
   * and internally call beginScheduler(...) on the local scheduler
   */
  public void resetTask(NRLProcessData processData) throws Throwable {
    final String taskName = processData.nextTaskName;
    final String wfName = processData.workflowName;
    final String namePrefix = wfName;

    final char sc = File.separatorChar;
    File              subDir      = new File(wfRootDir,"workflows"+sc+wfName+sc+taskName);
    RoutingInfo       theRouting  = SpecParser.parseRouting(new File(subDir,"routing"));
    HashSet           thePerms    = SpecParser.parseDataPerms(new File(subDir,"dataperm"));
    Hashtable         creates     = SpecParser.parseCreates(new File(subDir,"creates"));
    Hashtable         dTypes      = SpecParser.parseDataTypes(new File(subDir,"datatypes"));
    Hashtable         exceptions  = SpecParser.parseExceptions(new File(subDir,"exceptions"));
    Hashtable         fTypes      = SpecParser.parseFieldTypes(new File(subDir,"fieldtypes"));
    IRealizationInfo  rinfo       = SpecParser.parseRealization(new File(subDir,"realization"));
    

    final IScheduler _localScheduler;
    
    // BEGIN: Inherited from wfruntime.ServiceHost_Serv ///
    try {
      if (_serviceHost == null) _serviceHost = new NRLServiceHost(rmiPort, wfDefPath) {
        public void beginScheduler(String name, RoutingInfo theSpecs, HashSet thePerms, Hashtable creates,
            Hashtable dTypes, Hashtable exceptions, String namePrefix, ConfigInfo theConfig, Hashtable fTypes,
            IRealizationInfo rinfo) throws RemoteException {
          if (psl.survivor.ProcessorMain.debug) System.out.println("PSL! inside beginScheduler: creating new scheduler!!!");
          
          // BEGIN: Scheduler_Serv
          _scheduler = new Scheduler_Serv(name, theSpecs, thePerms, creates, 
              rmiPort, dTypes, exceptions, namePrefix, theConfig, fTypes, rinfo) {
                /**
                 * Inhertited from wfruntime.Scheduler_Serv
                 * - utilises the NRL-specific task-execution output to 
                 *   prepare the input data for the next task
                 */
            protected void fireNextTask(Output theOut, String state, Hashtable outBindings, Object instanceId) throws Throwable {
              if (psl.survivor.ProcessorMain.debug) System.out.println("fireNextTask ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
              if (!(theOut.getTask().equals("END"))) {
                Hashtable paramTable = outBindings;
                String originTask = this.name; // this task is the next 'originTask'
                NRLProcessData resultData = new NRLProcessData();    
                resultData.param = new HashMap();
                resultData.workflowName = wfName;          
                resultData.instanceId = instanceId;           
                resultData.originTask = originTask;        
                resultData.state = state;             
                resultData.paramTable = paramTable;
                resultData.nextTaskName = theOut.getTask();

                // instanceId.toString() will be used as key!
                if (psl.survivor.ProcessorMain.debug) System.out.println("FIRING NEXT TASK ***************************************************************************************************************************************");
                _resultDataStorage.put(instanceId.toString(), resultData);

              } else {
                // 2-do: transition to end task
                NRLProcessData resultData = new NRLProcessData();    
                resultData.workflowName = "END";

                _resultDataStorage.put(instanceId.toString(), resultData);
              }
            }   
          }; // ENDED: Scheduler_Serv
        }
        
      }; // ENDED: ServiceHost_Serv
      if (psl.survivor.ProcessorMain.debug) System.out.println("Successfully created ServiceHost_Serv: " + this);

    } catch (Throwable t) {
      t.printStackTrace();
    }
    // ENDED: Inherited from wfruntime.ServiceHost_Serv ///

    
    // the rmiService needs to have been setup by this point!
    IServiceHost theHost = (IServiceHost) Naming.lookup("rmi://localhost:" + rmiPort + "/serviceHost_" + _processorName);
    
    ConfigInfo theConfig = SpecParser.parseConfig(new File(wfRootDir, "workflows" + sc + wfName + sc + "config"));

    if (theRouting.getTaskType().equals("NonTransactionalTaskRealization") ||
        theRouting.getTaskType().equals("SynchronizationTaskIn") ||
        theRouting.getTaskType().equals("SynchronizationTaskOut")) {
      String[] files = subDir.list(new FilenameFilter() {
        public boolean accept(File file, String name) {
          return name.endsWith(".class");
        }
      });
      
      log("PSL! inside resetTask: going to invoke theHost.putRealizationInClasspath(...): theHost: " + theHost); // 2-do: remove
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
        log("PSL! inside resetTask: going to beginScheduler(...): namePrefix: " + namePrefix + ", theHost: " + theHost); // 2-do: remove
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
    
    try {
      Object instanceId = processData.instanceId;
      String originTask = processData.originTask;
      String state = processData.state;
      Hashtable paramTable = processData.paramTable;
      //      System.out.println("printing paramTable in resetTask: " + paramTable);

      // transition: moved in from executeTaskLocal
      _serviceHost._scheduler.transition(instanceId, originTask, state, paramTable);
    } catch (RemoteException re) {
      // left here for backwards compatibility
      // possibly thrown by: new Scheduler_Serv.transition(instanceId, originTask, state, paramTable);
      re.printStackTrace();        
    }
  }

  /**
   * load the bytecode for specified classFile necessary for execution
   */
  private static byte[] readClassFile(File theFile) throws Throwable {
    FileInputStream in = new FileInputStream(theFile);
    byte[] returnObj = new byte[(int)theFile.length()];
    in.read(returnObj);
    in.close();
    return returnObj;
  }

  // ENDED: Copied from wfruntime.psl.PSLWFLoader //////////////////////////////
  
  /**
   * helper class to achieve compatiblity with the NRL system
   */
  class NRLServiceHost extends ServiceHost_Serv {
    Scheduler_Serv _scheduler = null;
    NRLServiceHost(int rmiPort, String wfDefPath) throws Throwable {
      super(rmiPort, wfDefPath + "|" + _processorName);
    }
  }

    public static void setObject(String key, Object o) {
	_latestParam.put(key, o);
    }

    public static Object getObject(String key) { 
	return _latestParam.get(key); 
    }
  
}

