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
	for (int i = 0; i+1 < args.length; i+=2) {
	    if (args[i].equals("-f")) {
		xmlPath = args[i+1];
	    }
	    if (args[i].equals("-p")) {
		peerUrl = args[i+1];
	    }
	}
	
	if (xmlPath == null) { 
	    System.out.println("WRONG ARGUMENTS");
	}
	
	pm = new ProcessorBuilder(xmlPath);
	pm.createCloudNode(peerUrl);
    } 
}

