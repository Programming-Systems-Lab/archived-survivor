package psl.survivor.net;

import psl.worklets.WVM;

import java.io.Serializable;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Properties;

/**
 * @author Gaurav S. Kc [gskc@cs.columbia.edu]
 * @author Jean-Denis Greze [jg253@cs.columbia.edu]
 * 
 * Copyright (c) 2001: 
 * The Trustees of Columbia University and the City of New York.
 * All Rights Reserved.
 * 
 */

/**

 * This class is used to encapsulate the various 
 * task-processing of the given Processor
 */
class Capability implements Serializable {
  
  /** local host address */
  final String _hostIP;

  /** local WVM's rmiRegistration name */
  final String _wvmName;

  /** local WVM's port number */
  final int _wvmPort;

  /** local WVM's rmiRegistry port */
  final int _wvmRMIPort;

  /** used for loading capabilities/properties from a config file */
  Properties _properties;

  final TPTransportContainer _tptc;
  
  /**
   * Constructor
   */
  Capability(String file, TPTransportContainer tptc, WVM wvm) {
    _hostIP     = wvm.getWVMAddr();
    _wvmName    = wvm.getWVMName();
    _wvmPort    = wvm.getWVMPort();
    _wvmRMIPort = wvm.getRMIPort();
    
    if (file != null) {
      try {
        _properties = new Properties();
        _properties.load(new FileInputStream(file));
      } catch (FileNotFoundException fnfe) {
      } catch (IOException ioe) {
      }
    } else _properties = null;

    _tptc = tptc;
  }
  
  /**
   * returns a WVM-style URI for the local WVM
   */
  String toURL() {
    return _wvmName + "@" + _hostIP + ":" + _wvmPort;
  }
  
  /**
   * toString
   */
  public String toString() {
    return _wvmName + " @ " + _hostIP + " : " + _wvmPort;
  }
  
  /**
   * Comparision of equality with other Capability Objects
   * two Capability instances are equal only the WVM address
   * fields are equal
   */
  public boolean equals(Object o) {
    if (!(o instanceof Capability)) return false;
    Capability c = (Capability) o;
    return _hostIP.equals(c._hostIP) && _wvmRMIPort==c._wvmRMIPort &&
           (_wvmName.equals(c._wvmName) || _wvmPort==c._wvmPort);
  }
}
