package frysk.gui.srcwin.tags;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.SaveableXXX;

/**
 * A Tagset contains a collection of tags that are applicable to a process.
 * 
 * TODO: Tags not implemented yet
 * 
 * @author ajocksch
 *
 */
public class Tagset  extends GuiObject implements SaveableXXX{

	private String name;
	private String desc;
	private String command;
	private String version;
	
	private Vector tags;
	
	/**
	 * Creates a new tagset
	 * @param name The name of the tagset
	 * @param desc A brief description of the tagset
	 */
	public Tagset(String name, String desc, String command, String version){
		super(name,desc);
		this.name = name;
		this.desc = desc;
		this.command = command;
		this.version = version;
		this.tags = new Vector();
		doSaveObject();
	}
	
	public void save(Element node) {
		super.save(node);
		// Tag Sets
		node.setAttribute("command", this.command);
		node.setAttribute("version", this.version);
		Element tagsXML = new Element("tags");
		Iterator iterator = this.getTags();
		while (iterator.hasNext()) {
			Tag tag = (Tag) iterator.next();
			Element tagXML = new Element("tag");
			tag.save(tagXML);
			tagsXML.addContent(tagXML);
		}
		node.addContent(tagsXML);
	}
	
	public void load(Element node) {			
		super.load(node);
		
		//actions
		this.command = node.getAttribute("command").getValue();
		this.version = node.getAttribute("version").getValue();
		this.name = super.getName();
		this.desc = super.getToolTip();
	
		Element tagsXML = node.getChild("tags");
		List list = (List) (tagsXML.getChildren("tag"));
		Iterator i = list.iterator();
		Iterator j = this.getTags();
		
		while (i.hasNext()){
			((Tag)j.next()).load(((Element) i.next()));
		}

	}

	/**
	 * 
	 * @return The description of this tagset
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 
	 * @return The name of this tagset
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return The command that this tagset associates with
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * 
	 * @return The version of the command that this tagset is designed to be
	 * used with
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Adds the given tag to this tagset. If this tag is already in this Tagset an
	 * IllegalArgumentException is raised.
	 * @param newTag The tag to add
	 */
	public void addTag(Tag newTag){
		if(tags.contains(newTag))
			throw new IllegalArgumentException("Attempting to add a tag to a tagset it already belongs to");
		
		this.tags.add(newTag);
		doSaveObject();
	}
	
	/**
	 * 
	 * @return An iterator to all the tags in this tagset
	 */
	public Iterator getTags(){
		return this.tags.iterator();
	}
	
	/**
	 * Check for the presence of the provided tag in the tagset
	 * @param tag The tag to look for 
	 * @return true iff the tag was found
	 */
	public boolean containsTag(Tag tag){
		return this.tags.contains(tag);
	}
	
	/**
	 * Two tagsets are equal if they are the same size and contain the same
	 * tags. Order is irrelevant
	 */
	public boolean equals(Object obj){
		if(!(obj instanceof Tagset))
			return false;
		
		Tagset set2 = (Tagset) obj;
		
		// If they don't have the same size, they're not equal
		if(set2.tags.size() != this.tags.size())
			return false;
		
		// check the name first of all: that's more telling than the contents
		if(!this.name.equals(set2.name) || !this.version.equals(set2.version) ||
				!this.desc.equals(set2.desc))
			return false;
		
		
		boolean same = true;

		// Tags are unique within a set, if they are the same size only need to
		// make sure that every tag contained within set A appears in set B
		Iterator iter1 = this.tags.iterator();
		
		while(iter1.hasNext())
			same = same && set2.containsTag((Tag) iter1.next());
		
		
		return same;
	}
}
