package psl.survivor.net;

import java.io.Serializable;

import java.util.Vector;
import java.util.ArrayList;

import psl.survivor.util.Version;
import psl.survivor.proc.TaskProcessorHandle;

public class VTransportContainer implements Serializable {
    private String _name;
    private String _hostname;
    private int _port;  
    private Version _v;
    private int _state;
    private Vector _ve;
    private ArrayList _al;
    private String _sName, _sHostname;
    private int _sPort;
    private TaskProcessorHandle _tph;
    private int _identifier;
    public VTransportContainer(String name, String hostname, int port) {
	_state = 0;
	_name = name;
	_hostname = hostname;
	_port = port;
    } 

    public void setName(String name) { _name = name; }
    public void  setHostName(String hostname) { _hostname = hostname; }
    public void setPort(int port) { _port = port; }
    public void setSourceName(String name) { _sName = name; }
    public void  setSourceHostName(String hostname) { _sHostname = hostname; }
    public void setSourcePort(int port) { _sPort = port; }
    public void setSource(String name, String hostname, int port) 
    { _sName = name; _sHostname = hostname; _sPort = port; }

    public String getName() { return _name; }
    public String getHostName() { return _hostname; }
    public int getPort() { return _port; }
    public String getSourceName() { return _sName; }
    public String getSourceHostName() { return _sHostname; }
    public int getSourcePort() { return _sPort; }

    public void setAlertExecutingTask(Version v) { _state = 1; _v = v;}
    public void setAlertDoneExecutingTask(Version v) { _state = 2; _v = v;}
    public void setReplicate(Version v, Vector rq) 
    { _state = 3; _v = v; _ve = rq; }
    public void setMediate(Version v) { _state = 4; _v = v;}
    public void setValid(int i) { _state = 5; _identifier = i;}
    public void setPing(Version v, int i) { _state = 6; _v = v; }
    public void setPing2(int i) { _state = 6; _identifier = i; }
    public void setExecuteTask(Version v) { _state = 7; _v = v; }
    public void setFindRemoteProcessor(Version v, ArrayList al) 
    { _state = 8; _v = v; _al = al; }
    public void setPingResponse() { _state = 9; }
    public void setValidResponse() { _state = 10; }
    public void setReplicatorPing() { _state = 11; }
    public void setReplicatorPingResponse() { _state = 12; }
    public void setAddToCloud(TaskProcessorHandle tph) 
    { _state = 13; _tph = tph;}
    public void setSendNewHandle(TaskProcessorHandle tph) 
    { _state = 14; _tph = tph;}
    public void setSendPool(ArrayList al) { _state = 15; _al = al; }

    public boolean isAlertExecutingTask() { return _state == 1; }
    public Version getAlertExecutingTaskVersion() { return _v; }
    public boolean isAlertDoneExecutingTask() { return _state == 2; }
    public Version getAlertDoneExecutingTaskVersion() { return _v; }
    public boolean isReplicate() { return _state == 3; }
    public Version getReplicatedVersion() { return _v; }
    public Vector  getReplicatedQueue() { return _ve; }
    public boolean isMediate() { return _state == 4; }
    public Version getMediateVersion() { return _v; }
    public boolean isValid() { return _state == 5; }
    public int getIdentifier() { return _identifier; }
    public boolean isPing() { return _state == 6; }
    public Version getPing() { return _v; }
    public boolean isExecuteTask() { return _state == 7; }
    public Version getExecuteTask() { return _v; }
    public boolean isFindRemoteProcessor() { return _state == 8; }
    public Version getFindRemoteProcessor1() { return _v; }
    public ArrayList getFindRemoteProcessor2() { return _al; }
    public boolean isPingResponse() { return _state == 9; }
    public boolean isValidResponse() { return _state == 10; }
    public boolean isReplicatorPing() { return _state == 11; }
    public boolean isReplicatorPingResponse() { return _state == 12; }
    public boolean isAddToCloud() { return _state == 13; }
    public TaskProcessorHandle getAddToCloud() { return _tph; }
    public boolean isSendNewHandle() { return _state == 14; }
    public TaskProcessorHandle getSendNewHandle() { return _tph; }
    public boolean isSendPool() { return _state == 15; }
    public ArrayList getSendPool() { return _al; }
}
