package psl.survivor.xml;

import java.io.*;
import java.util.ArrayList;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import psl.survivor.util.*;
import psl.survivor.proc.*;
import psl.survivor.net.*;

import java.lang.ClassNotFoundException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.InstantiationException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Constructor;

/**
 * Builds Processors out of xml file with processor capabilities.
 * JDOM is being used for xml parsing purposes.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class ProcessorBuilder {

  private SAXParser _sxp = null;

  // we may make multiple processors, though only the first one
  // is set up to run currently.
  private ArrayList _processors = null;

  
  public ProcessorBuilder(String xmlPath) {
    _sxp = new SAXParser();
    _sxp.setContentHandler(new ProcessorHandler(this));
    try {
      _sxp.parse(new InputSource
        (new FileInputStream(xmlPath)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  
  // start all the processors
  for (int i = 0; i < _processors.size(); i++) {
      Processor p = ((Processor)_processors.get(i));
      Thread t = new Thread(p);
      t.start();
    }
  }

    /**
     * Once we are done building all processors, we can build the cloud
     */
  public void createCloudNode(String peer) {
    Processor p = ((Processor)_processors.get(0)); // 1 processor only
    MessageHandler mh = null;
    
    // setup replicator
      Replicator r;
    CloudNode cn = new CloudNode(peer, new TPTransportContainer(p), 
                                 mh = new MessageHandler
      (p, r = new Replicator(p.getName(), p)));
    Thread t = new Thread(r);
    t.start(); // start the replicator

    p.addMainReplicator(r);
    p.setMessageHandler(mh);
  }

  public Processor getFirstProcessor() {
    return (Processor)_processors.get(0);
  }
  
  public void log(String s) {
    System.err.println(s);
  }

  public void log(int i) {
    System.err.println(i);
  }

    /**
     * The callback class for the xml parsing
     */
  class ProcessorHandler extends DefaultHandler {
    private ProcessorBuilder _pm;
      private int _depth; 
      // keep track of how deep in the xml
      // tree we currently are
    
      // we actually dynamically figure out the processor type.
      // this is only here as a default
    private String _processorType = "psl.survivor.proc.Processor";
    private String _processorName = "default";
    private int _tcpPort = 8883;
    private String _rmiName = "";
    private String _wfDefPath = "";
    private ArrayList _capabilities = new ArrayList();
    private ArrayList _processors = new ArrayList();

    /** CTOR */
    public ProcessorHandler(ProcessorBuilder pm) {
      _pm = pm;
      _depth = 0;
    }
    public void startElement(String uri, String localName,
                             String qName,
                             Attributes attributes) 
      throws SAXException {
      if (localName.equals("TaskProcessor")) {
        if (_depth == 0) {
          String s;
          s = attributes.getValue("", "proctype");
          if (s != null) _processorType = s;
          s = attributes.getValue("", "name");
          if (s != null) _processorName = s;
          s = attributes.getValue("", "TCPPort");
          if (s != null) _tcpPort = Integer.parseInt(s);
          s = attributes.getValue("", "RMIName");
          if (s != null) _rmiName = s;
          s = attributes.getValue
            ("", "WorkflowDefinitionPath");
          if (s != null) _wfDefPath = s;
        } else {
	    // _pm.log("bad");
        }
      } else if (localName.equals("Capability")) {
	  if (_depth == 1) { // setup capabilities
          String s1, s2; 
          s1 = attributes.getValue("", "name");
          s2 = attributes.getValue("", "value");
          if ((s1 != null) && (s2 != null)) {
            NameValuePair nv = 
                              new NameValuePair(s1, s2);
            _capabilities.add(nv);
          }
        } else {
	    // _pm.log("bad");
        }
      }
      _depth++;
    }
    public void endElement(String namespaceURI, 
                           String localName, String qName)
      throws SAXException {
      _depth--;
      // we we get an endelement for a processor, we can make
      // an instance of it
      if (localName.equals("TaskProcessor")) {
        if (_depth == 0) {

          final Class [] parameterTypes = new Class[] { String.class, int.class, String.class, String.class };
          final Object [] parameterVals = new Object[] { _processorName, new Integer(_tcpPort), _rmiName, _wfDefPath };
          Class procClass = null;
          Constructor constr = null;
          Processor p = null;
          
          try {
            procClass = Class.forName(_processorType);
            constr = procClass.getConstructor(parameterTypes);
            p = (Processor) constr.newInstance(parameterVals);
          } catch (ClassNotFoundException cnfr) {
          } catch (NoSuchMethodException nsme) {
          } catch (SecurityException se) {
          } catch (InstantiationException ie) {
          } catch (IllegalAccessException iae) {
          } catch (IllegalArgumentException iae) {
          } catch (InvocationTargetException ite) {
          }
          
          for (int i = 0; i < _capabilities.size(); i++) {
            p.addCapability(_capabilities.get(i));
          }
          _processors.add(p);
          _capabilities.clear();
          _processorName = "default";
          _tcpPort = 8883;
          _rmiName = "";
          _wfDefPath = "";		    
        } else {
	    // _pm.log("bad");
        }
      } else if (localName.equals("Capability")) {
        if (_depth == 1) {
        } else {
	    // _pm.log("bad");
        }
      }
    }

    public void endDocument() {
      _pm._processors = _processors;
      
    }
  }
}



