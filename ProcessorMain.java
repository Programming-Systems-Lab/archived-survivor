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
import java.util.Vector;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import psl.survivor.xml.ProcessorBuilder;
import psl.survivor.proc.*;

public class ProcessorMain {
    public static boolean debug = false;
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

	    if (args[i].equals("-d")) {
	    	// DEBUG mode
		debug = true;
	    }
	}
	
	if (xmlPath == null) { 
	    System.out.println("usage: java ProcessorMain -f <.xml> -n <rName> -h <rHost> -p <rPort> -l <port> -d <dbg>");
	    return;
	}
	
	pm = new ProcessorBuilder(xmlPath);
	pm.createCloudNode(peerUrl);

	// Let's add a replicator to the Processor
	Processor theProc = pm.getFirstProcessor();
	/*
	  Replicator r = new Replicator(theProc.getName()+"_Rep", theProc);
	  Thread t = new Thread(r);
	  t.start();
	  theProc.addMainReplicator(r);
	*/

	if ((name != null) && (hostname != null) && (port != -1)) {
	    // TODO do the stuff for getting in touch with a remoteHost
	    TaskProcessorHandle tph = 
					new TaskProcessorHandle(name, hostname, port);
	    Processor p = pm.getFirstProcessor();
	    
	    tph.addToCloud(p);
	}

        setupListener(lPort, pm.getFirstProcessor());
    } 
    private static final String CMD_SHUTDOWN = "shutdown";    
    private static final String CMD_STARTWF = "start wf: ";    
    private static final String CMD_CLOUD = "cloud";
    private static final String CMD_QUIT = "quit";
    private static final String CMD_REPLICATORS = "rep";

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

		while (true) {
		    s.getOutputStream().write("\nYo: ".getBytes());
		    s.getOutputStream().flush();
		    
		    InputStream is = s.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader br = new BufferedReader(isr);
		    String command = br.readLine().toLowerCase();
		    
		    if (command.startsWith(CMD_SHUTDOWN)) {
			System.out.println("Received a shutdown request");
			System.exit(0);
		    /*
		    } else if (command.startsWith(CMD_STARTWF)) {
			System.out.println("Received a start request");
			proc.startWorkflow(command.substring(CMD_STARTWF.length()));
		    */
		    } else if (command.startsWith(CMD_CLOUD)) {
			System.out.println("Received a cloud status request");
			PoolData pd = proc.getPoolData();
			System.out.println(pd.toString());
		    } else if (command.startsWith(CMD_REPLICATORS)) {
			Vector v = proc.getReplicators();
			for (int i = 0; i <v.size(); i++) {
			    System.out.println(v.get(i));
			}
		    } else if (command.startsWith(CMD_QUIT)) {
			break;
		    } else if (command.startsWith("s")) {
			if (psl.survivor.ProcessorMain.debug) System.out.println("TEMPORARY SHORTCUT!!! Received a start request");
			proc.startWorkflow("svr1-1");
		    }
		    System.out.println("received command: " + command);
		
		}

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

