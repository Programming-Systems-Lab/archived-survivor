import java.io.*;
import java.util.ArrayList;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

public class ProcessorMain {
    
    private SAXParser sxp = null;
    ArrayList _processors = null;

    private ProcessorMain(String xmlPath) {
	System.out.println("xmlPath: " + xmlPath);
	
	sxp = new SAXParser();
	sxp.setContentHandler(new ProcessorHandler(this));
	try {
	    sxp.parse(new InputSource
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
	CloudNode cn = new CloudNode(peer, new TPTransportContainer(), new MessageHandler());
	// peer = rmi@hostname:port

    }

    public void log(String s) {
	System.err.println(s);
    }

    public void log(int i) {
	System.err.println(i);
    }

    class ProcessorHandler extends DefaultHandler {
	private ProcessorMain _pm;
	private int _depth;

	private String _processorName = "default";
	private int _wvmPort = 8882;
	private int _tcpPort = 8883;
	private String _wfDefPath = "";
	private ArrayList _capabilities = new ArrayList();
	private ArrayList _processors = new ArrayList();

	/** CTOR */
	public ProcessorHandler(ProcessorMain pm) {
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
		    s = attributes.getValue("", "WVMPort");
		    if (s != null) _wvmPort = Integer.parseInt(s);
		    s = attributes.getValue("", "TCPPort");
		    if (s != null) _tcpPort = Integer.parseInt(s);
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
		    Processor p = new Processor
			(_processorName, _wvmPort, _tcpPort,
			 _wfDefPath);
		    for (int i = 0; i < _capabilities.size(); i++) {
			p.addCapability(_capabilities.get(i));
		    }
		    _processors.add(p);
		    _capabilities.clear();
		    _processorName = "default";
		    _wvmPort = 8882;
		    _tcpPort = 8883;
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

    public static void main(String[] args) {
	ProcessorMain pm = null;
	
	String name = null;
	String xmlPath = null;
	String peerURL = null;
	for (int i = 0; i+1 < args.length; i+=2) {
	    if (args[i].equals("-f")) {
		xmlPath = args[i+1];
	    }
	    if (args[i].equals("-p")) {
		peerURL = args[i+1];
	    }
	}
	
	if (xmlPath == null) { 
	    System.out.println("WRONG ARGUMENTS");
	}
	
	pm = new ProcessorMain(xmlPath);
	pm.createCloudNode(pm);
    } 
}

