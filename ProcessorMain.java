package psl.survivor;

import java.io.*;
import java.util.ArrayList;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import psl.survivor.xml.ProcessorBuilder;

public class ProcessorMain {
    public static void main(String[] args) {
	ProcessorBuilder pm = null;
	
	String name = null;
	String xmlPath = null;
	String peerUrl = null;
	String name = null;
	String hostname = null;
	int port = -1;
	for (int i = 0; i+1 < args.length; i+=2) {
	    if (args[i].equals("-f")) {
		xmlPath = args[i+1];
	    }
	    if (args[i].equals("-p")) {
		peerUrl = args[i+1];
	    }
	    if (args[i].equals("-n")) {
		name = args[i+1];
	    }
	    if (args[i].equals("-h")) {
		hostname = args[i+1];
	    }
	    if (args[i].equals("-p")) {
		port = Integer.parseInt(args[i+1]);
	    }
	}
	
	if (xmlPath == null) { 
	    System.out.println("WRONG ARGUMENTS");
	}
	
	pm = new ProcessorBuilder(xmlPath);
	pm.createCloudNode(peerUrl);
	if ((name != null) && (hostname != null) && (port != -1)) {
	    // TODO do the stuff for getting in touch with a remoteHost
	    TaskProcessorHandle tph = 
		new TaskProcessorHandle(name, hostname, port);
	    Processor p = pm.getFirstProcessor();
	    tph.addToCloud(p);
	}
    } 
}

