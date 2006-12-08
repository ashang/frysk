import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Bug
    implements Comparable
{
    private final File file;
    private final String id;
    private final int hash;
    Bug (File file)
    {
	this.file = file;
	this.id = file.getName ().split ("\\.")[0];
	this.hash = Integer.parseInt (this.id);
    }
    /**
     * Is the object equals to this one.
     */
    public boolean equals (Object o)
    {
	return ((Bug)o).hash == hash;
    }
    /**
     * Return the hash code for this ID (hash on the underlying ID
     * value).
     */
    public int hashCode ()
    {
	return hash;
    }
    /**
     * Assuming that the two objects are the same, do a relative
     * comparison.
     */
    public int compareTo (Object o)
    {
	Bug rhs = (Bug)o;
	return rhs.hash - this.hash;
    }
    /**
     * The bug as a simplified string.
     */
    public String toString ()
    {
	return id;
    }
    public String toPrint ()
    {
	return ""
	    + id
	    + " " + reporter
	    + " " + duplicate
	    + " " + dependson
	    + " " + blocked
	    + " " + description;
    }


    String bug_id = "?";
    String description = "";
    List blocked = new LinkedList ();
    List dependson = new LinkedList ();
    boolean duplicate = false;
    String reporter;
    String assigned_to;

    private void walk (Map map, Node parent)
    {
	for (Node child = parent.getFirstChild ();
	     child != null;
	     child = child.getNextSibling ()) {
	    if (child.getNodeType () == Node.TEXT_NODE
		&& parent.getNodeType () == Node.ELEMENT_NODE) {
		// Grab the bug-id as it flies by.
		String name = parent.getNodeName ();
		String value = child.getNodeValue ();
		if (name.equals ("bug_id"))
		    bug_id = value;
		if (name.equals ("short_desc"))
		    description = description + value;
		// Print anything this blocks
		if (name.equals ("blocked"))
		    blocked.add (map.get (value));
		if (name.equals ("dependson"))
		    dependson.add (map.get (value));
		if (name.equals ("resolution"))
		    duplicate = value.equals("DUPLICATE");
		if (name.equals ("reporter"))
		    reporter = value;
		if (name.equals ("assigned_to"))
		    assigned_to = value;
	    }
	    walk (map, child);
	}
    }

    private static final DocumentBuilderFactory documentBuilderFactory
	= DocumentBuilderFactory.newInstance();
    public void parse (Map map)
    {
	try {
	    DocumentBuilder documentBuilder
		= documentBuilderFactory.newDocumentBuilder ();
	    String file = id + ".xml";
	    Document document = documentBuilder.parse (file);
	    walk (map, document);
	}
	catch (javax.xml.parsers.ParserConfigurationException e) {
	    throw new RuntimeException (e);
	}
	catch (org.xml.sax.SAXException e) {
	    throw new RuntimeException (e);
	}
	catch (java.io.IOException e)  {
	    throw new RuntimeException (e);
	}
	catch (gnu.xml.dom.ls.DomLSException e) {
	    System.err.println (id + " parse error");
	}
    }

    public static TreeMap slurp ()
    {
	TreeMap map = new TreeMap ();
	File[] dotXml = new File (".").listFiles (new FilenameFilter ()
	    {
		public boolean accept (File path, String filename)
		{
		    return filename.matches ("^[0-9]*.xml$");
		}
	    });
	System.out.println ("Loading bugs");
	for (int i = 0; i < dotXml.length; i++) {
	    File file = dotXml[i];
	    Bug bug = new Bug (file);
	    map.put (bug.id, bug);
	}
	System.out.println ("Parsing bugs");
	for (Iterator i = map.values ().iterator ();
	     i.hasNext (); ) {
	    Bug bug = (Bug)i.next ();
	    bug.parse (map);
	    System.out.println (bug.id + " " + bug.description);
	}
	return map;
    }

    public static void main (String[] args)
    {
	TreeMap map = slurp ();

	{
	    System.out.println ("Roots:");
	    Collection roots = ((Map) (map.clone())).values ();
	    for (Iterator i = roots.iterator ();
		 i.hasNext (); ) {
		Bug bug = (Bug)i.next ();
		if (bug.blocked.size () > 0)
		    i.remove ();
	    }
	    for (Iterator i = roots.iterator (); i.hasNext (); ) {
		Bug bug = (Bug)i.next ();
		System.out.println ("Root: " + bug.toPrint ());
	    }
	}

	{
	    System.out.println ("Duplicates:");
	    for (Iterator i = map.values ().iterator (); i.hasNext (); ) {
		Bug bug = (Bug)i.next ();
		if (!bug.duplicate)
		    continue;
		if (bug.dependson.size () > 0)
		    System.out.println ("dependson: " + bug.toPrint ());
		if (bug.blocked.size () != 1)
		    System.out.println ("blocked: " + bug.toPrint ());
	    }
	}
    }
}
