package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;

public class DOMGetter {
	
	private Document data;
	
	public DOMGetter(Document data){
		this.data = data;
	}
	
	public Iterator getImages(){
		Iterator i = this.data.getRootElement().getChildren().iterator();
		Vector v = new Vector();		
		
		while(i.hasNext()){
			Element elem = (Element) i.next();
			v.add(new DOMImage(elem));
		}
		
		return v.iterator();		
	}
	
	public int getPID(){
		return Integer.parseInt(this.data.getRootElement().getAttribute("pid").getValue()); 
	}
}
