package psl.survivor.xml;

import java.io.*;
import java.util.ArrayList;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import psl.survivor.proc.*;
import psl.survivor.util.*;

/**
 * Used to parse a TaskDefinition xml file and build 
 * TaskDefinitions out of it.
 * JDOM is used for xml parsing.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */

public class TaskDefinitionBuilder {
    private SAXParser sxp = null;
    ArrayList _taskDefinitions = null;

    public TaskDefinitionBuilder(String xmlPath) {
	// log("xmlPath: " + xmlPath);
	
	sxp = new SAXParser();
	sxp.setContentHandler(new TaskDefinitionHandler(this));
	try {
	    sxp.parse(new InputSource
		(new FileInputStream(xmlPath)));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }  

    public void log(String s) {
	if (psl.survivor.ProcessorMain.debug) System.err.println(s);
    }

    public void log(int i) {
	if (psl.survivor.ProcessorMain.debug) System.err.println(i);
    }

    public ArrayList getTaskDefinitions() { return _taskDefinitions; }

    class TaskDefinitionHandler extends DefaultHandler {
	private TaskDefinitionBuilder _tdb;
	private int _depth;
	private String _name = "default";
	private ArrayList _taskDefinitions = new ArrayList();
	private ArrayList _requirements = new ArrayList();

	public TaskDefinitionHandler(TaskDefinitionBuilder tdb) {
	    _tdb = tdb;
	    _depth = 0;
	}

	public void startElement(String uri, String localName,
				 String qName,
				 Attributes attributes) 
	    throws SAXException {
	    if (localName.equals("WorkflowDefinitino")) {
		if (_depth == 0) {
		} else {
		    // _tdb.log("bad");
		}
	    }
	    if (localName.equals("Task")) {
		if (_depth == 1) {
		    String s = attributes.getValue("", "name");
		    if (s != null) _name = s;
		} else {
		    // _tdb.log("bad");
		}
	    } else if (localName.equals("Requirement")) {
		if (_depth == 2) {
		    String s1, s2;
		    s1 = attributes.getValue("", "name");
		    s2 = attributes.getValue("", "value");
		    if ((s1 != null) && (s2 != null)) {
			NameValuePair nv = 
			    new NameValuePair(s1, s2);
			_requirements.add(nv);
		    }
		} else {
		    // _tdb.log("bad");
		}
	    }
	    _depth++;
	}

	public void endElement(String namespaceURI, 
			       String localName, String qName)
	    throws SAXException {
	    _depth--;
	    if (localName.equals("WorkflowDefinition")) {
		if (_depth == 0) {
		} else {
		    // _tdb.log("bad");
		}
	    } else if (localName.equals("Task")) {
		if (_depth == 1) {
		    TaskDefinition td = new TaskDefinition(_name);
		    for (int i = 0; i < _requirements.size(); i++) {
			td.addRequirement(_requirements.get(i));
		    }
		    _taskDefinitions.add(td);
		    _requirements.clear();
		    _name = "default";
		} else {
		    // _tdb.log("bad");
		}
	    } else if (localName.equals("Requirement")) {
		if (_depth == 2) {
		} else {
		    // _tdb.log("bad");
		}
	    }
	}

	public void endDocument() {
	    _tdb._taskDefinitions = _taskDefinitions;
	}
    }
}
