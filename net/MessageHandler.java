package psl.survivor.net;

import java.util.HashSet;
import java.util.Date;

import psl.survivor.proc.Processor;
import psl.survivor.proc.Replicator;


/**
 * This class is used to send a receive message from worklets to the 
 * Processor and other psl.survivor components.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 */
public class MessageHandler {

    private long _WAITFORPING = 5000; // 5 seconds
    private long _WAITFORVALID = 5000; // 5 seconds
    private long _WAITFORREPLICATORPING = 5000; // 5 seconds


    // The following are used to keep track of ping and valid requests
    // and responses
    private int _validIdentifier = 0;
    private int _pingIdentifier = 0;
    private int _replicatorPingIdentifier = 0;
    private HashSet _pingRequests;
    private HashSet _validRequests;
    private HashSet _replicatorPingRequests;
    
    // handle to the Processor and the Replicator
    private Processor _processor;
    private Replicator _replicator;

    // used for communication via worklets
    private CloudNode _cloudNode;


    /** 
     * CTOR
     *
     * When creating a Message Handler, it is important to have a
     * Processor and a Replicator so that the message handler can do
     * callbacks to the survivor system */
    public MessageHandler(Processor p, Replicator r) { 
	_processor = p;
	_replicator = r;
	_pingRequests = new HashSet();
	_validRequests = new HashSet();
	_replicatorPingRequests = new HashSet();
    }


    /** Set the component that we are using for communication */
    public void setCloudNode(CloudNode cn) { _cloudNode = cn; }


    /* Handle an incoming message */
    public void handleMessage(Object o) {
	if (psl.survivor.ProcessorMain.debug) System.out.println("&&&&&&&&& Received Message:\n"+o);

	// If it is a standard survivor message
	if (o instanceof VTransportContainer) {
	    VTransportContainer t = (VTransportContainer) o;
	    if (t.isPingResponse()) {
		t.setName(t.getSourceName());
		t.setHostName(t.getSourceHostName());
		t.setPort(t.getSourcePort());
		t.setSourceName(_processor.getName());
		t.setSourceHostName(_processor.getHostName());
		t.setSourcePort(_processor.getPort());
		Integer ti = new Integer(t.getIdentifier());
		synchronized(_pingRequests) {
		    if (_pingRequests.contains(ti)) {
			_pingRequests.remove(ti);
		    } else {
			if (psl.survivor.ProcessorMain.debug) System.err.println("does not contain ping response?");
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
			if (psl.survivor.ProcessorMain.debug) System.err.println("does not contain valid response?");
			if (psl.survivor.ProcessorMain.debug) System.err.println(ti);
		    }
		}
	    } else if (t.isReplicatorPingResponse()) {
		t.setName(t.getSourceName());
		t.setHostName(t.getSourceHostName());
		t.setPort(t.getSourcePort());
		t.setSourceName(_processor.getName());
		t.setSourceHostName(_processor.getHostName());
		t.setSourcePort(_processor.getPort());
		Integer ti = new Integer(t.getIdentifier());
		synchronized(_replicatorPingRequests) {
		    if (_replicatorPingRequests.contains(ti)) {
			_replicatorPingRequests.remove(ti);
		    } else {
			if (psl.survivor.ProcessorMain.debug) System.err.println("does not contain ping reponse?");
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
	    } else if (t.isStopTask()) {
		_processor.stopTask(t.getStopTask());
            } else if (t.isShutdown()) {
		_processor.shutdown();
	    }

	    // other, as of yet unsupported, message types
	    // for now, we do everything using the VTC
	} else if (o instanceof RTransportContainer) { // replicator
	} else if (o instanceof TPTransportContainer) { // task processor
	}
    }    


    /** Ping another server to see if it is up */
    public boolean sendPing(VTransportContainer t) {

	// first make sure it's a ping message we are sending
	if (t.isPing()) {

	    // add to our ping requests, so that we know
	    // when we have gotten a ping response
	    Integer ti = new Integer(_pingIdentifier++);
	    synchronized(_pingRequests) {
		_pingRequests.add(ti);
		if (psl.survivor.ProcessorMain.debug) System.err.println(ti);
	    }
	    t.setPing2(_pingIdentifier-1);
	    sendMessage(t);
	    Date start = new Date();
	    Date now = new Date();

	    // check that we haven't waited too long
	    while (now.getTime() - start.getTime() < _WAITFORPING) {
		synchronized(_pingRequests) {
		    if (!_pingRequests.contains(t)) {
			return true; // we got a response, ping is ok
		    }
		}
		now = new Date();
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    ;
		}
	    }
	    synchronized(_pingRequests) {
		if (_pingRequests.contains(ti)) {
		    _pingRequests.remove(ti);
		}
	    }
	} else {
	    if (psl.survivor.ProcessorMain.debug) System.err.println("not a ping request but should be");
	}
	return false; // no response after wait interval, ping is not ok
    }


    /** Validate another server to see that it is up */
    public boolean sendValid(VTransportContainer t) {

	// first check to see we are sending the correct message type
	if (t.isValid()) {
	    Integer ti = new Integer(_validIdentifier++);

	    // keep track of our requests
	    synchronized(_validRequests) {
		_validRequests.add(ti);
		if (psl.survivor.ProcessorMain.debug) System.err.println(ti);
	    }
	    t.setValid(_validIdentifier-1);
	    sendMessage(t);  
	    Date start = new Date();
	    Date now = new Date();

	    // wait for a response
	    while (now.getTime() - start.getTime() < _WAITFORVALID) {
		synchronized(_validRequests) {
		    if (!_validRequests.contains(ti)) {
			return true; // got response all is well
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
	    if (psl.survivor.ProcessorMain.debug) System.err.println("not a \"valid\" request but should be");
	}
	return false; // no response, not valid
    }

    /** Same as ping, but for replicators as opposed to processors */
    public boolean sendReplicatorPing(VTransportContainer t) {
	if (t.isReplicatorPing()) {
	    Integer ti = new Integer(_replicatorPingIdentifier++);
	    synchronized (_replicatorPingRequests) {
		_replicatorPingRequests.add(ti);
	    }
	    t.setReplicatorPing2(_replicatorPingIdentifier-1);
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
	    if (psl.survivor.ProcessorMain.debug) System.err.println("not a replicatorping request but should be");
	}
	synchronized(_replicatorPingRequests) {
	    if (_replicatorPingRequests.contains(t)) {
		_replicatorPingRequests.remove(t);
	    }
	}
	return false;
    }


    /** Send a message using the CloudNode */
    public void sendMessage(VTransportContainer t) {
	if (psl.survivor.ProcessorMain.debug) System.out.println("&&&&&&&&&&&&&&&& Sending Message:\n " + t);
	t.setSource(_processor.getName(),
		    _processor.getHostName(),
		    _processor.getPort());
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }

    
    /** Send a message using the CloudNode */
    public void sendMessage(TPTransportContainer t) {
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }


    /** Send a message using the CloudNode */
    public void sendMessage(RTransportContainer t) {
	String peer = t.getName()+"@"+t.getHostName()+":"+t.getPort();
	_cloudNode.sendMessage(peer, t);
    }
}
