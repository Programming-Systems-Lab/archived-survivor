package psl.survivor.net;

import java.util.ArrayList;
import java.io.Serializable;


/**
 * Transport container for Replicator messages.
 *
 * Currently not used. VTransport Container used for all communications */
 *
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 * @author Jean-Denis Greze (jg253@cs.columbiae.edu)
 */
public class RTransportContainer implements Serializable {
    private String _name;
    private String _rmiName;
    private String _hostname;
    private int _port;
    private ArrayList _capabilities;
    public RTransportContainer(String name, String hostname, int port, 
			       String rmiName) {
	_name = name;
	_hostname = hostname;
	_port = port;
	_rmiName = rmiName;
    }
    public void addCapabilities(Object o) {
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
