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

/**
 * Used to start a node capabale of running a workflow.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav Kc (gskc@cs.columbia.edu)
 */
public class ProcessorMain {

    public static boolean debug = false;

    // GSKC THIS IS FOR YOU
    public static psl.survivor.proc.Log THELOG = null;

    /**
     * MAIN
     *
     * Arguments:
     * -f <filename> = xml file describing capabilities of this processor
     * -n <name> = of processor we are going to connect to. Processor
     *             executing workflows are connected.
     * -h <hostname> = of processor we are going to connect to.
     * -p <port> = of processor we are going to connect to.
     * -l <port> = local port that this processor will listen to. this is
     *             used to communicate to a processor that we want it to
     *             take some action such as starting the workflow.
     * -d d = use debug mode
     */
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
	
	// Create a processor
	pm = new ProcessorBuilder(xmlPath);
	pm.createCloudNode(peerUrl);

	// If we are to connect to another processor
	if ((name != null) && (hostname != null) && (port != -1)) {
	    TaskProcessorHandle tph = new TaskProcessorHandle
		(name, hostname, port);
	    Processor p = pm.getFirstProcessor();
	    tph.addToCloud(p);
	}

	if (lPort != -1)
	    setupListener(lPort, pm.getFirstProcessor());
    } 

    // commands that can be used on the listenning port
    private static final String CMD_SHUTDOWN = "shutdown";    
    private static final String CMD_SHUTDOWN_ALL = "shutdown all";
    private static final String CMD_STARTWF = "start wf: ";    
    private static final String CMD_CLOUD = "cloud";
    private static final String CMD_QUIT = "quit";
    private static final String CMD_REPLICATORS = "rep";


    /**
     * Setup a local port where the processor awaits for commands such
     * as that to start the workflow.
     */
    private static void setupListener(final int port, final Processor proc) {
      if (port <= 0) return;

      try {

        final ServerSocket listener = new ServerSocket(port);
        (new Thread() {
          public void run() {
            try {
              while (true) {
                Socket s = listener.accept();

		while (true) {
		    s.getOutputStream().write
			("\n[Processor Prompt] ".getBytes());
		    s.getOutputStream().flush();
		    
		    InputStream is = s.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    BufferedReader br = new BufferedReader(isr);
		    String command = br.readLine().toLowerCase();
		    
		    if (command.startsWith(CMD_SHUTDOWN)) {
			System.out.println("Received a shutdown request");
			System.exit(0);
		    } else if (command.startsWith(CMD_SHUTDOWN_ALL)) {
			System.out.println("Received a shutdown-all request");
			System.exit(0);
			/* } else if (command.startsWith(CMD_STARTWF)) {
			   System.out.println("Received a start request");
			   proc.startWorkflow(command.substring(CMD_STARTWF.length())); */
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





