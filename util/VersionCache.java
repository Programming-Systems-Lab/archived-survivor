package psl.survivor.util;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

/**
 * Way to keep track of versions and possible clashes. This provides
 * for a way to get "latest" versions of a workflow in case we need
 * to survive a partial workflow execution.
 *
 * @author Jean-Denis Greze (jg253@cs.columbia.edu)
 * @author Gaurav S. Kc (gskc@cs.columbia.edu)
 */
public class VersionCache {

    private TreeSet _set;

    /**
     * CTOR
     */
    public VersionCache() {
	_set = new TreeSet();
    }

    /**
     * add a version to the cache (only if it does not already contain it)
     */
    public void addVersion(Version v) {
	if (!_set.contains(v)) {
	    _set.add(v);
	}
    }

    /**
     * see if the cache contains a certain version
     */
    public boolean contains(Version v) {
	return _set.contains(v);
    }

    /**
     * get the latest version <= to v
     */
    public Version getLatestVersion(Version v) {
	if (_set.contains(v)) {
	    return (Version)_set.tailSet(v).first();
	}
	else {
	    return (Version)_set.headSet(v).last();
	}
    }

    /**
     * get the latest version that also has the largest possible common
     * root with v.
     */
    public Version getLatestVersionWithCommonRoot(Version v) {
	if (_set.contains(v)) {
	    return (Version) _set.tailSet(v).first();
	}
	Iterator it = _set.iterator();
	Version best = null;
	Version bestRoot = null;
	while (it.hasNext()) {
	    Version ve = (Version) it.next();
	    if (ve.compareTo(v) < 0) {
		if  (ve.commonRoot(v).compareTo(bestRoot) > 0) {
		    best = ve;
		    bestRoot = ve.commonRoot(v);
		}
	    } else {
		break;
	    }
	}
	return best;
    }

    /**
     * expire all version older than v, as well as v
     */
    public void expireOlderThan(Version v) {
	SortedSet ss = _set.tailSet(v);
	Iterator it = ss.iterator();
	if (it.hasNext()) {
	    it.next();
	}
	while (it.hasNext()) {
	    _set.remove(it.next());
	}
    }

    /*
     * expire all version younger than v, not v
     */
    public void expireYoungerThan(Version v) {
	SortedSet ss = _set.headSet(v);
	Iterator it = ss.iterator();
	while (it.hasNext()) {
	    _set.remove(it.next());
	}
    }

    /**
     * expire v (aka remove it from the cache)
     */
    public void expire(Version v) {
	if (_set.contains(v)) {
	    _set.remove(v);
	}
    }

    /**
     * get all versions younger than v (not including v)
     */
    public SortedSet getAllLaterVersions(Version v) {
	SortedSet ss = new TreeSet(_set.tailSet(v));
	ss.remove(v);
	return ss;
    }

    /**
     * get all versions younger than v (including v)
     */
    public SortedSet getAllLaterVersionsInclusive(Version v) {
	SortedSet ss =_set.tailSet(v);
	return ss;
    }
    
    /**
     * String representation of cache
     */
    public String toString() {
	String returnValue = "VersionCache:\n";
	Iterator it = _set.iterator();
	while (it.hasNext()) {
	    returnValue += it.next().toString() + "\n";
	}
	returnValue += "----------\n";
	return returnValue;
    }

    /**
     * main: test Version and VersionCache
     */
    public static void main(String[] args) {
	System.out.println("test Version:");

	Version v0 = new Version(new Integer(0));
       	System.out.println(v0);
	v0.append("a");
	System.out.println(v0);
	v0.append("b");
	System.out.println(v0);
	for (int i = 0; i < v0.size(); i++) {
	    System.out.println("+" + v0.getIdentifier(i));
	}
	
	Version v1 = new Version(new Integer(1));
	v1.append("a");
	System.out.println(v1.commonRoot(v0));
	System.out.println(v0.commonRoot(v1));

	Version v2 = new Version(new Integer(2));
	System.out.println(v2.commonRoot(v0));
	System.out.println(v0.commonRoot(v2));

	if (v0.compareTo(v2) > 0) System.out.println("OK");
	else System.out.println("BAD");
	
	if (v1.compareTo(v0) < 0) System.out.println("OK");
	else System.out.println("BAD");

	v1.append("b");
	
	if (v1.compareTo(v0) == 0) System.out.println("OK");
	else System.out.println("BAD");

	if (v1.equals(v0)) System.out.println("OK");
	else System.out.println("BAD");

	Version v3 = (Version) v0.clone();

	if (v0.equals(v3)) System.out.println("OK");
	else System.out.println("BAD");

	Version v4 = v0.split(new Integer(4));
	if (v0.equals(v4)) System.out.println("OK");
	else System.out.println("BAD");
	
	Version v5 = new Version(new Integer(5));
	v5.append ("a");
	v5.append ("d");

	if (v5.compareTo(v0) > 0) System.out.println("OK");
	else System.out.println("BAD");

	if (v0.compareTo(v5) < 0) System.out.println("OK");
	else System.out.println("BAD");

	System.out.println(v0);
	System.out.println(v1);
	System.out.println(v2);
	System.out.println(v3);
	System.out.println(v4);
	System.out.println(v5);

	System.out.println("\ntest VersionCache:");

	VersionCache vc = new VersionCache();

	vc.addVersion(v0);
	vc.addVersion(v1);
	vc.addVersion(v2);
	vc.addVersion(v3);
	vc.addVersion(v4);
	vc.addVersion(v5);

	System.out.println(vc);
	
	if (vc.contains(v0)) System.out.println("OK");
	else System.out.println("BAD");
	if (vc.contains(v1)) System.out.println("OK");
	else System.out.println("BAD");
	if (vc.contains(v2)) System.out.println("OK");
	else System.out.println("BAD");
	if (vc.contains(v3)) System.out.println("OK");
	else System.out.println("BAD");
	if (vc.contains(v4)) System.out.println("OK");
	else System.out.println("BAD");
	if (vc.contains(v5)) System.out.println("OK");
	else System.out.println("BAD");

	Version v6 = new Version(new Integer(6));
	v6.append("a");
	v6.append("c");

	System.out.println(vc.getLatestVersion(v6));
	System.out.println(vc.getLatestVersion(v5));
	System.out.println(vc.getLatestVersion(v4));
	System.out.println(vc.getLatestVersion(v1));
	System.out.println(vc.getLatestVersion(v2));

	System.out.println(vc.getAllLaterVersions(v6));
	System.out.println(vc.getAllLaterVersions(v2));
	System.out.println(vc.getAllLaterVersionsInclusive(v2));
	System.out.println(vc);

	System.out.println("all tests completed Dec. 8th, 2001");
	System.out.println("\ntest VersionCache (2):");
	VersionCache vc2 = new VersionCache();
	Version p0 = new Version("p0");
	Version p1 = new Version("p1");
	Version p2 = new Version("p2");
	Version p3 = new Version("p3");

	Version p4 = new Version("p4");
	Version p5 = new Version("p5");

	p1.append("B");
	p1.append("B");
	p1.append("C");

	p2.append("B");
	p2.append("A");

	p3.append("A");

	p4.append("B");
	p4.append("B");
	p4.append("C");
	p4.append("D");

	p5.append("F");

	vc2.addVersion(p0);
	vc2.addVersion(p1);
	vc2.addVersion(p2);
	vc2.addVersion(p3);

	System.out.println(vc2.getLatestVersionWithCommonRoot(p1));
	System.out.println(p1);

	System.out.println(vc2.getLatestVersionWithCommonRoot(p4));

	System.out.println(vc2.getLatestVersionWithCommonRoot(p5));

	System.out.println("all tests completed Dec. 10th, 2001");
    }
}
