package psl.survivor.net;

import psl.worklets.WVM;

import java.net.ServerSocket;
import java.net.SocketException;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import java.io.Serializable;

/**
 * @author Gaurav S. Kc [gskc@cs.columbia.edu]
 * @author Jean-Denis Greze [jg253@cs.columbia.edu]
 * 
 * Copyright (c) 2001: 
 * The Trustees of Columbia University and the City of New York.
 * All Rights Reserved.
 * 
*/

public class CloudNode implements Runnable {
  private final WVM _wvm;
  private Capability _capability;

  /** 
   * Used for 2 main purposes:
   *  - pinging between peer nodes
   *  - other remote-admin stuff
   * 
   * Did not use UDP DatagramSocket
   *  because the remote-admin stuff
   *  wouldn't work though UDP
   * 
  */

  /* DISCONTINUED USE ... 
  private final int _controlPort;
  private ServerSocket _controlSocket = null;
  private Thread _pingThread = null;
  */

  /** MessageHandler for local host */
  private final MessageHandler _msgHandler;

  /** listing of peer CloudNodes in the network / vicinity */
  private final Vector _peers;

  /** listing of nodes that replicate my data */
  private final Vector _masterOf;

  /** listing of nodes that I'm replicating data for */
  private final Vector _slaveFor;

  /** storage of data resultant from task completion */
  private final Hashtable _data;
  
  private static final String CAPABILITY_REQ = "WHAT'S YOUR CAPABILITY";
  private static final String CAPABILITY_RESP = "THAT'S MY CAPABILITY";
  
  private static final String REPLICATE_REQ = "PLEASE REPLICATE MY DATA";
  private static final String RETRIEVE_REQ = "PLEASE REPLICATE MY DATA";

  private static final String GENERAL_MESSAGE = "GENERAL MESSAGE";

  public CloudNode(String peerURL, TPTransportContainer tptc,
         MessageHandler mh) {
    //GAURAV
    // can you add this constructor above, thanks
    // this (null, null, 0, null, null, null, null);

    // jeandenis
    // is this what you mean?
      
      // oh yeah that -1 is such a freakin' hack
    this(tptc.getHostName(), tptc.getName(), tptc.getPort()-1, peerURL, null, tptc, mh);
  }
  
  public CloudNode(String host, String name, int port,
                    String peerURL,
                    TPTransportContainer tptc,
                    MessageHandler mh) {
    this(host, name, port, null, peerURL, tptc, mh);
  }

  private CloudNode(String capabilityFile, String peerURL) {
    this(null, null, 0, capabilityFile, peerURL, null, null);
  }

  private CloudNode(String host, String name, int port, String peerURL,
                    String capabilityFile,
                    TPTransportContainer tptc,
                    MessageHandler mh) {

    _wvm = new WVM(this, host, name, port);
    _capability = new Capability(capabilityFile, tptc, _wvm);

    _msgHandler = mh;

    _peers = new Vector();
    _masterOf = new Vector();
    _slaveFor = new Vector();
    _data = new Hashtable();

    if (_msgHandler != null) {
	_msgHandler.setCloudNode(this);
    }
    
    // add own capability into local 'knowledge-base' of network
    _peers.addElement(_capability);

    if (psl.survivor.ProcessorMain.debug) System.out.println("WVMPORT:" + _wvm.getWVMPort());
    
    _wvm.requestHandler = new Runnable() {
      public void run() {
        Integer i = new Integer(Thread.currentThread().hashCode());
        Object messageType = _wvm.messageQueueKeys.get(i);
        Object uniqueKey = _wvm.messageQueueMsgs.get(i);
        if (messageType.equals(CAPABILITY_REQ)) {
          // send out my list of nodes' capabilities
          _wvm.messageQueueMsgs.put(uniqueKey, _peers);
          
        } else if (messageType.equals(CAPABILITY_RESP)) {
          // update my list of nodes' capabilities w/ this new upstart ;)
          Capability c = (Capability) _wvm.messageQueueMsgs.get(uniqueKey);
          _peers.insertElementAt(c, 1);
          System.out.println("Current nodes: " + printCapabilities());
          
        } else if (messageType.equals(REPLICATE_REQ)) {

          new Exception().printStackTrace(WVM.out);
          WVM.out.println("THIS SHOULD NOT HAPPEN!!! " + REPLICATE_REQ);

          // someone asked me to replicate their data
          CompositeData cd = (CompositeData) _wvm.messageQueueMsgs.get(uniqueKey);
          // 2-do: replace this by proper object type for "ReplicatedData"
          // extract from this object:
          //  - uniqwe key: used to identify 'where' this data came from
          //    basically, need a mapping from 'data' object <->
          //    meaning/location of data
          //  - actual data to be replicated
          Object key = cd.getKey(), replicatedData = cd.getData();
          // 2-do: .. don't remember what i was going to put here :(
          // need to associate an existing Capability object with this request
          _data.put(key, replicatedData);
          // might want to return an ACK to the sender!
        
        } else if (messageType.equals(RETRIEVE_REQ)) {

          new Exception().printStackTrace(WVM.out);
          WVM.out.println("THIS SHOULD NOT HAPPEN!!! " + RETRIEVE_REQ);

          // someone asked for data that I've replicated for someone
          CompositeData cd = (CompositeData) _wvm.messageQueueMsgs.get(uniqueKey);
          Object key = cd.getKey(), replicatedData = _data.get(key);
          // need to associate an existing Capability object with this request
          Capability c = null; // find out the real peer
          _wvm.messageQueueMsgs.get(replicatedData);

        } else if (messageType.equals(GENERAL_MESSAGE)) {
          Object o = _wvm.messageQueueMsgs.get(uniqueKey);
          if (_msgHandler != null) _msgHandler.handleMessage(o);

        } else {
          // 2-do: silly-ass comment
        }
      }
    };
    
    if (peerURL != null) {
      // send own capability to peer
      _wvm.sendMessage(CAPABILITY_RESP, _capability, peerURL);
      
      // retrieve peer's capability
      Enumeration e; boolean sameAsPeerURL = true;
      for (e = ((Vector) _wvm.getMessage(CAPABILITY_REQ, peerURL)).elements();
           e!=null && e.hasMoreElements(); ) {
        Capability c = (Capability) e.nextElement();
        if (! _peers.contains(c)) {
          _peers.addElement(c);
          if (! c.equals(peerURL)) {
            if (sameAsPeerURL) {
              // first entry will be equivalent to peerURL
              sameAsPeerURL = false;
              continue;
            }
            _wvm.sendMessage(CAPABILITY_RESP, _capability, c.toURL());
          }
        }
      }
    }
    /* DISCONTINUED USE ... 
    try {
      _controlSocket = new ServerSocket(0);
      _controlSocket.setSoTimeout(WAIT_TIMEOUT);
    } catch (SocketException se) {
      WVM.out.println("Unable to open control socket: " + se);
      se.printStackTrace(WVM.out);
      System.exit(-1);
    }    
    _controlPort = _controlSocket.getLocalPort();    
    (_pingThread = new Thread(this, "CloudNode-pingThread")).start();
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        shutdown();
      }
    });
    */
  }

  public boolean sendMessage(String peerURL, Serializable s) {
    return _wvm.sendMessage(GENERAL_MESSAGE, s, peerURL);
  }

  private static final int WAIT_TIMEOUT = 1000;
  private boolean _shuttingDown = false;
  
  public void run() {
    /* DISCONTINUED USE ... 
    while (! _shuttingDown) {
      try {
        Socket s = _controlSocket.accept();
      } catch (IOException ioe) {
        WVM.out.println("Problem with ping socket: " + se);
        se.printStackTrace(WVM.out);
        System.exit(-1);
      }
    }
    */
  }
  void shutdown() {
    if (_wvm.isAlive()) _wvm.shutdown();
    /* DISCONTINUED USE ... 
    _shuttingDown = true;
    _pingThread.join(WAIT_TIMEOUT*2);
    _controlSocket.close();
    */
  }
  
  private String printCapabilities() {
    String peers = "";
    for (Enumeration e = _peers.elements(); e.hasMoreElements(); )
      peers += e.nextElement() + "\n";
    return peers;
  }
    
  public String toString() {
    return "CloudNode: " + 
           // DISCONTINUED USE ... "\n - control socket: " + _controlPort +
           "\n - wvm: " + printCapabilities();
  }
  
  public static void main(String args[]) {
    WVM.out.println("usage: java wfruntime.psl.CloudNode " + 
                       "[-c <capabilities>] " +
                       "[-p <peer>] " +
                       "[-h]");
    int i = -1;
    String capabilities = null, peer = null;
    while (++i < args.length) {
      if (args[i].equals("-c")) capabilities = args[++i];
      else if (args[i].equals("-p")) peer = args[++i];
      else if (args[i].equals("-h")) {
        usage();
        return;
      }      
    }
    WVM.out.print(new CloudNode(capabilities, peer));
  }
  
  private static void usage() {
    WVM.out.println("usage: java psl.survivor.net.CloudNode " +
                    "[-c <capabilities file>]" +
                    "[-p <peer WVM URL>]" +
                    "[-h]");
  }
}
