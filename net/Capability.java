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
class Capability implements Serializable {
  final String _hostIP;
  final String _wvmName;
  final int _wvmPort;
  final int _wvmRMIPort;
  Properties _properties;

  final TPTransportContainer _tptc;
  
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
  
  String toURL() {
    return _wvmName + "@" + _hostIP + ":" + _wvmPort;
  }
  public String toString() {
    return _wvmName + " @ " + _hostIP + " : " + _wvmPort;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof Capability)) return false;
    Capability c = (Capability) o;
    return _hostIP.equals(c._hostIP) && _wvmRMIPort==c._wvmRMIPort &&
           (_wvmName.equals(c._wvmName) || _wvmPort==c._wvmPort);
  }
}
