package psl.survivor.net;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * Eventually this will all be xml, but not for now.
 */
public class TPTransportContainer implements Serializable {
    private String _name;
    private String _hostname;
    private int _port;
    private ArrayList _capabilities;
    public TPTransportContainer(String name, String hostname, int port) {
	_name = name;
	_hostname = hostname;
	_port = port;
    }    
    public void addCapabilities(Object o) {
	if (!_capabilities.contains(o)) {
	    _capabilities.add(o);
	}
    }
}
