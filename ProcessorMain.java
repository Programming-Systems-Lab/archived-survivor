package psl.survivor;

import java.io.*;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import java.util.ArrayList;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import psl.survivor.xml.ProcessorBuilder;
import psl.survivor.proc.*;

public class ProcessorMain {
    public static void main(String[] args) {
	ProcessorBuilder pm = null;
	
	String name = null;
	String xmlPath = null;
	String peerUrl = null;
	String hostname = null;
	int port = -1;
        int lPort = -1;
	for (int i = 0; i+1 < args.length; i+=2) {
	    if (args[i].equals("-f")) {
		xmlPath = args[i+1];
	    }
      /*
	    if (args[i].equals("-p")) {
		peerUrl = args[i+1];
	    }
      */
	    if (args[i].equals("-n")) {
		name = args[i+1];
	    }
            
	    if (args[i].equals("-h")) {
		hostname = args[i+1];
	    }

	    if (args[i].equals("-p")) {
		port = Integer.parseInt(args[i+1]);
	    }

	    if (args[i].equals("-l")) {
                // TCP listener port
		lPort = Integer.parseInt(args[i+1]);
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

        setupListener(lPort, null);
    } 

    private static void setupListener(final int port, final Processor proc) {
      if (port <= 0) return;

      try {
        System.out.println("listening on socket: " + port);

        final ServerSocket listener = new ServerSocket(port);
        (new Thread() {
          public void run() {
            try {
              while (true) {
                Socket s = listener.accept();

                s.getOutputStream().write("Yo: ".getBytes());
                s.getOutputStream().flush();

                InputStream is = s.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String command = br.readLine().toLowerCase();

                if (command.startsWith("shutdown")) {
                  System.out.println("Received a shutdown request");
                  System.exit(0);
                } else if (command.startsWith("start wf")) {
                  System.out.println("Received a start request");
                  // todo: uncomment proc.startWorkflow();
                }

                System.out.println("received command: " + command);
                s.close();
              }
            } catch (IOException ioe) {
              ioe.printStackTrace();
            }
          }
        }).start();

      } catch (UnknownHostException uhe) {
        uhe.printStackTrace();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
}

