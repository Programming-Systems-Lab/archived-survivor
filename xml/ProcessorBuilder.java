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

public class ProcessorBuilder {
    private SAXParser _sxp = null;
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
	for (int i = 0; i < _processors.size(); i++) {
	    Processor p = ((Processor)_processors.get(i));
	    Thread t = new Thread(p);
	    t.start();
	}
    }

    public void createCloudNode(String peer) {
	Processor p = ((Processor)_processors.get(0)); // 1 processor only
        MessageHandler mh = null;
	
  // System.out.println("Creating a cloudNode: " + peer);
	CloudNode cn = new CloudNode(peer, new TPTransportContainer(p), 
				     mh = new MessageHandler
					 (p, new Replicator(p.getName(), p)));
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

    class ProcessorHandler extends DefaultHandler {
	private ProcessorBuilder _pm;
	private int _depth;

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
		    _pm.log("bad");
		}
	    } else if (localName.equals("Capability")) {
		if (_depth == 1) {
		    String s1, s2;
		    s1 = attributes.getValue("", "name");
		    s2 = attributes.getValue("", "value");
		    if ((s1 != null) && (s2 != null)) {
			NameValuePair nv = 
			    new NameValuePair(s1, s2);
			_capabilities.add(nv);
		    }
		} else {
		    _pm.log("bad");
		}
	    }
	    _depth++;
	}
	public void endElement(String namespaceURI, 
			       String localName, String qName)
	    throws SAXException {
	    _depth--;
	    if (localName.equals("TaskProcessor")) {
		if (_depth == 0) {
		    Processor p = new psl.survivor.proc.nrl.NRLProcessor
			(_processorName, _tcpPort, _rmiName,
			 _wfDefPath);
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
		    _pm.log("bad");
		}
	    } else if (localName.equals("Capability")) {
		if (_depth == 1) {
		} else {
		    _pm.log("bad");
		}
	    }
	}

	public void endDocument() {
	    _pm._processors = _processors;
	    
	}
    }
}
