package psl.survivor.util;

import java.io.Serializable;

public class NameValuePair implements Comparable, Serializable {
    private Comparable _name;
    private Comparable _value;
    public NameValuePair(Comparable name, Comparable value) {
	_name = name;
	_value = value;
    }
    public Comparable getName() { return _name; }
    public Comparable getValue() { return _value; }
    public boolean equals(Object o) {
	return compareTo(o)==0;
    }
    public int compareTo(Object o) {
	if (o instanceof NameValuePair) {
	    NameValuePair nv = (NameValuePair) o;
	    if (_name.compareTo(nv.getName()) == 0) {
		return _value.compareTo(nv.getValue());
	    } else {
		return _name.compareTo(nv.getName());
	    }
	} else {
	    return -1;
	}
    }
    public String toString() {
	return "["+ _name.toString() + ":" + _value.toString() + "]";
    }
}
