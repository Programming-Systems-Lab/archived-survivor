package psl.survivor.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

import psl.survivor.proc.Processor;

/**
 * Container for communicating the addresses of handles.
 *
 * Eventually this will all be xml, but not for now.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 */
public class TPTransportContainer implements Serializable {


    private String _name;
    private String _hostname;
    private int _port;
    private ArrayList _capabilities;


    /** Create a TC for a Processor and its capabilities.
     */
    public TPTransportContainer(Processor p) {
	_name = p.getName();
	_hostname = p.getHostName();
	_port = p.getPort();
	ArrayList c = p.getCapabilities();
  _capabilities = new ArrayList();
	synchronized (c) {
	    Iterator it = c.iterator();
	    while (it.hasNext()) {
		addCapability(it.next());
	    }
	}
    }


    public TPTransportContainer(String name, String hostname, int port) {
	_name = name;
	_hostname = hostname;
	_port = port;
  _capabilities = new ArrayList();
    }    
    public void addCapability(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	}
    }


    public String getName() { return _name; }
    public String getHostName() { return _hostname; }
    public int getPort() { return _port; }
    public ArrayList getCapabilities() { return _capabilities; }
    public int getSize() { return _capabilities.size(); }
    public Object getCapability(int i) { return _capabilities.get(i); }
}
