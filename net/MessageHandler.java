package psl.survivor.net;

import java.util.HashSet;
import java.util.Date;

import psl.survivor.proc.Processor;
import psl.survivor.proc.Replicator;

public class MessageHandler {
    private long _WAITFORPING = 5000; // 5 seconds
    private long _WAITFORVALID = 5000; // 5 seconds
    private long _WAITFORREPLICATORPING = 5000; // 5 seconds

    private int _validIdentifier = 0;
    
    private Processor _processor;
    private Replicator _replicator;
    private HashSet _pingRequests;
    private HashSet _validRequests;
    private HashSet _replicatorPingRequests;
    private CloudNode _cloudNode;
    /** CTOR */
    public MessageHandler(Processor p, Replicator r) { 
	_processor = p;
	_pingRequests = new HashSet();
	_validRequests = new HashSet();
	_replicatorPingRequests = new HashSet();
    }
    public void setCloudNode(CloudNode cn) { _cloudNode = cn; }
    public void handleMessage(Object o) {
	System.out.println("&&&&&&&&& Received Message:\n"+o);
	if (o instanceof VTransportContainer) {
	    VTransportContainer t = (VTransportContainer) o;
	    if (t.isPingResponse()) {
		t.setName(t.getSourceName());
		t.setHostName(t.getSourceHostName());
		t.setPort(t.getSourcePort());
		t.setSourceName(_processor.getName());
		t.setSourceHostName(_processor.getHostName());
		t.setSourcePort(_processor.getPort());
		synchronized(_pingRequests) {
		    if (_pingRequests.contains(t)) {
			_pingRequests.remove(t);
		    } else {
			System.err.println("does not contain ping response?");
		    }
		}
	    } else if (t.isValidResponse()) {
		t.setName(t.getSourceName());
		t.setHostName(t.getSourceHostName());
		t.setPort(t.getSourcePort());
		t.setSourceName(_processor.getName());
		t.setSourceHostName(_processor.getHostName());
		t.setSourcePort(_processor.getPort());
		Integer ti = new Integer(t.getIdentifier());
		synchronized(_validRequests) {
		    if (_validRequests.contains(ti)) {
			_validRequests.remove(ti);
		    } else {
			System.err.println("does not contain valid response?");
			System.err.println(ti);
		    }
		}
	    } else if (t.isReplicatorPingResponse()) {
		t.setName(t.getSourceName());
		t.setHostName(t.getSourceHostName());
		t.setPort(t.getSourcePort());
		t.setSourceName(_processor.getName());
		t.setSourceHostName(_processor.getHostName());
		t.setSourcePort(_processor.getPort());
		synchronized(_replicatorPingRequests) {
		    if (_replicatorPingRequests.contains(t)) {
			_replicatorPingRequests.remove(t);
		    } else {
			System.err.println("does not contain ping reponse?");
		    }
		}
	    } else if (t.isReplicatorPing()) {
		if (_replicator.ping()) {
		    t.setReplicatorPingResponse();
		    t.setName(t.getSourceName());
		    t.setHostName(t.getSourceHostName());
		    t.setPort(t.getSourcePort());
		    t.setSourceName(_processor.getName());
		    t.setSourceHostName(_processor.getHostName());
		    t.setSourcePort(_processor.getPort());
		    sendMessage(t);
		}
	    } else if (t.isPing()) {
		if (_processor.ping()) {
		    t.setPingResponse();
		    t.setName(t.getSourceName());
		    t.setHostName(t.getSourceHostName());
		    t.setPort(t.getSourcePort());
		    t.setSourceName(_processor.getName());
		    t.setSourceHostName(_processor.getHostName());
		    t.setSourcePort(_processor.getPort());
		    sendMessage(t);
		}
	    } else if (t.isValid()) {
		if (_processor.valid()) {
		    t.setValidResponse();
		    t.setName(t.getSourceName());
		    t.setHostName(t.getSourceHostName());
		    t.setPort(t.getSourcePort());
		    t.setSourceName(_processor.getName());
		    t.setSourceHostName(_processor.getHostName());
		    t.setSourcePort(_processor.getPort());
		    sendMessage(t);
		}
	    } else if (t.isAlertExecutingTask()) {
		_replicator.alertExecutingTask
		    (t.getAlertExecutingTaskVersion());
		
	    } else if (t.isAlertDoneExecutingTask()) {
		_replicator.alertDoneExecutingTask
		    (t.getAlertDoneExecutingTaskVersion());
	    } else if (t.isReplicate()) {
		_replicator.replicated(t.getReplicatedVersion(),
				       t.getReplicatedQueue());
	    } else if (t.isMediate()) {
		// TODO implement this and make sure it gets used
	    } else if (t.isExecuteTask()) {
		_processor.executeTask(t.getExecuteTask());
	    } else if (t.isFindRemoteProcessor()) {
		_processor.findRemoteProcessor
		    (t.getFindRemoteProcessor1(),
		     t.getFindRemoteProcessor2());
	    } else if (t.isAddToCloud()) {
		_processor.alertNewHandle(t.getAddToCloud());
	    } else if (t.isSendNewHandle()) {
		_processor.addProcessor(t.getSendNewHandle());
	    } else if (t.isSendPool()) {
		_processor.addPool(t.getSendPool());
	    }
	} else if (o instanceof RTransportContainer) {
	} else if (o instanceof TPTransportContainer) {
	}
    }    
    public boolean sendPing(VTransportContainer t) {
	if (t.isPing()) {
	    synchronized(_pingRequests) {
		_pingRequests.add(t);
	    }
	    sendMessage(t);
	    Date start = new Date();
	    Date now = new Date();
	    while (now.getTime() - start.getTime() < _WAITFORPING) {
		synchronized(_pingRequests) {
		    if (!_pingRequests.contains(t)) {
			return true;
		    }
		}
	    }
	} else {
	    System.err.println("not a ping request but should be");
	}
	synchronized(_pingRequests) {
	    if (_pingRequests.contains(t)) {
		_pingRequests.remove(t);
	    }
	}
	return false;
    }
    public boolean sendValid(VTransportContainer t) {
	if (t.isValid()) {
	    Integer ti = new Integer(_validIdentifier++);
	    synchronized(_validRequests) {
		_validRequests.add(ti);
		System.err.println(ti);
	    }
	    t.setValid(_validIdentifier-1);
	    sendMessage(t);
	    Date start = new Date();
	    Date now = new Date();
	    while (now.getTime() - start.getTime() < _WAITFORVALID) {
		synchronized(_validRequests) {
		    if (!_validRequests.contains(ti)) {
			return true;
		    }
		}
		now = new Date();
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    ;
		}
	    }
	    synchronized(_validRequests) {
		if (_validRequests.contains(ti)) {
		    _validRequests.remove(ti);
		}
	    }
	} else {
	    System.err.println("not a \"valid\" request but should be");
	}
	return false;
    }
    public boolean sendReplicatorPing(VTransportContainer t) {
	if (t.isReplicatorPing()) {
	    synchronized (_replicatorPingRequests) {
		_replicatorPingRequests.add(t);
	    }
	    sendMessage(t);
	    Date start = new Date();
	    Date now = new Date();
	    while (now.getTime() - start.getTime() < _WAITFORREPLICATORPING) {
		synchronized(_replicatorPingRequests) {
		    if (!_replicatorPingRequests.contains(t)) {
			return true;
		    }
		}
		now = new Date();
	    }
	} else {
	    System.err.println("not a replicatorping request but should be");
	}
	synchronized(_replicatorPingRequests) {
	    if (_replicatorPingRequests.contains(t)) {
		_replicatorPingRequests.remove(t);
	    }
	}
	return false;
    }
    public void sendMessage(VTransportContainer t) {
	System.out.println("&&&&&&&&&&&&&&&& Sending Message:\n " + t);
	t.setSource(_processor.getName(),
		    _processor.getHostName(),
		    _processor.getPort());
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }
    public void sendMessage(TPTransportContainer t) {
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }
    public void sendMessage(RTransportContainer t) {
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }
}
