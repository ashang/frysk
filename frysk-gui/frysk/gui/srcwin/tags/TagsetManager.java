package frysk.gui.srcwin.tags;

import java.util.Iterator;
import java.util.Vector;

/**
 * The TagsetManager keeps track of the tagsets available for frysk,
 * as well as maintaining the association between tagsets and what
 * executable command they are applicable to.
 * @author ajocksch
 *
 */
public class TagsetManager {
	
	// singleton
	public static TagsetManager manager;
	
	static{
		// initialize our singleton
		manager = new TagsetManager();
	}
	
	private Vector tagsets;
	
	/**
	 * Create a new TagsetManager
	 *
	 */
	public TagsetManager(){
		tagsets = new Vector();
	}
	
	/**
	 * Adds the tagset to the manager. If the tagset already exists in the
	 * manager an IllegalArgumentException is thrown. 
	 * @param toAdd The tagset to store.
	 */
	public void addTagset(Tagset toAdd){
		if(tagsets.contains(toAdd)){
			throw new IllegalArgumentException("Tagset " + toAdd.getName() + " was already in manager");
		}
		
		this.tagsets.add(toAdd);
	}
	
	/**
	 * Returns all of the tagsets applicable to the provided command
	 * @param command The command to get the tagsets for
	 * @return The tagsets applicable to command.
	 */
	public Iterator getTagsets(String command){
		Iterator iter = tagsets.iterator();
		
		Vector matches = new Vector();
		
		while(iter.hasNext()){
			Tagset set = (Tagset) iter.next();
			if(set.getCommand().equals(command))
				matches.add(set);
		}
		
		return matches.iterator();
	}
}
