package psl.survivor.net;
import java.util.ArrayList;
import java.io.Serializable;
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
}
