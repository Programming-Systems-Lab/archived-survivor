package psl.survivor.util;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * 
 */
public class Version implements Comparable, Serializable {

    private Object _data;
    private ArrayList _identifiers;

    /**
     * CTOR
     * o is the Object that this version refers to
     */
    public Version(Object o) {
	_identifiers = new ArrayList();
	_data = o;
    }

    /**
     * returns the data that this version is associated with
     */
    public Object data() {
	return _data;
    }

    /**
     * Appends an identifier
     */
    public void append(Comparable o) {
	_identifiers.add(o);
    }

    /**
     * returns the ith identifier. Throws exception if beyond bounds
     */
    public Object getIdentifier(int i) 
	throws IndexOutOfBoundsException {
	if ((i > _identifiers.size()) || (i < 0)) {
	    throw new IndexOutOfBoundsException("" +i);
	}
	return _identifiers.get(i);
    }

    /**
     * Returns the number of identifiers
     */
    public int size() {
	return _identifiers.size();
    }

    /**
     * Returns a Version object w/ the largest common identifier root
     * between this and v. Note that this version object's data is null.
     */
    public Version commonRoot(Version v) {
	Version returnValue = new Version(null);
	int size = (_identifiers.size() < v.size())?
	    _identifiers.size():v.size();
	for (int i = 0; i < size; i++) {
	    if (!_identifiers.get(i).equals(v.getIdentifier(i))) {
		return returnValue;
	    }
	    returnValue.append(((Comparable)_identifiers.get(i)));
	}
	return returnValue;
    }

    /**
     * Implementation of the Comparable interface.
     * If o !instanceof Version, we return -1
     * If o == null, we return 1
     */
    public int compareTo(Object o) {
	if (o instanceof Version) {
	    Version v = (Version)o;
	    if (v.size() < this.size()) return 1;
	    if (v.size() > this.size()) return -1;
	    int size = v.size();
	    for (int i = 0; i < size; i++) {
		if (((Comparable)_identifiers.get(i)).compareTo
		    (v.getIdentifier(i)) < 0) {
		    return -1;
		} else if (((Comparable)_identifiers.get(i)).compareTo
			   (v.getIdentifier(i)) > 0) {
		    return 1;
		}
	    }
	    return 0;
	} else {
	    if (o == null) {
		return 1;
	    } else {
		return -1;
	    }
	}
    }

    /**
     * returns true if these two versions have the most basic type
     * of same ancestry.
     */
    public boolean hasCommonRoot(Version v) {
	if ((_identifiers.size() > 0) && (v.size() > 0)) {
	    if (_identifiers.get(0).equals(v.getIdentifier(0))) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns true iff o instanceof Version, o.size() == this.size(), 
     * and forall identifiers, o.getIdentifier(i) == this.getIdentifier(i)
     */
    public boolean equals(Object o) {
	return compareTo(o) == 0;
    }

    /**
     * @return copy of the object
     */
    public Object clone() {
	Version returnValue = new Version(_data);
	for (int i = 0; i < _identifiers.size(); i++) {
	    returnValue.append((Comparable)_identifiers.get(i));
	}
	return returnValue;
    }

    /**
     * Clone this version, and make clone version that now refers to object o
     */
    public Version split(Object o) {
	Version returnValue = new Version(o);
	for (int i = 0; i < _identifiers.size(); i++) {
	    returnValue.append((Comparable)_identifiers.get(i));
	}
	return returnValue;
    }
    
    /**
     * String representation of the object. Its length and the string 
     * representation of every identifier in the object in order.
     */
    public String toString() {
	String returnValue =  "-- "+_identifiers.size();
	for (int i = 0; i < _identifiers.size(); i++) {
	    returnValue += " : " +_identifiers.get(i);
	}
	returnValue += " => " + _data;	    
	return returnValue;
    }
}
