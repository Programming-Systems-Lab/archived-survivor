package psl.survivor.util;

import java.io.Serializable;

/**
 * It is often important to store name value pairs of data. This does it.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc @cs.columbia.edu
 */
public class NameValuePair implements Comparable, Serializable {


    private Comparable _name;


    private Comparable _value;


    /** CTOR */
    public NameValuePair(Comparable name, Comparable value) {
	_name = name;
	_value = value;
    }


    /** Get the name of this pair */
    public Comparable getName() { return _name; }


    /** Get the value of this pair */
    public Comparable getValue() { return _value; }


    /** checks for equality between this NVP and some other
        object. Only returns true if the object is an NVP with same
        name and value. */
    public boolean equals(Object o) {
	return compareTo(o)==0;
    }


    /** Compare a NVP to another. If we try to compare the NVP to some
        other object, then this returns -1. */
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

    
    /** String representation of a name value pair */
    public String toString() {
	return "["+ _name.toString() + ":" + _value.toString() + "]";
    }
}
