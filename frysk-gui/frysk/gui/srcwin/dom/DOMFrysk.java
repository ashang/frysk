package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;
import java.math.BigInteger;
import org.jdom.Document;
import org.jdom.Element;

/**
 * DOMFrysk serves as an access point to the document object model for the frysk
 * source window.
 * 
 * @author ajocksch
 */

public class DOMFrysk {

	/**
	 * The pid of the process this DOM represents
	 */
	private static final String PID_ATTR = "pid";
	
	private static final Element pidValue = new Element(PID_ATTR);

	private static final String PC_ATTR = "PC";

	private static final String value = "value";

	private static final String CCPATH_ATTR = "CCPATH";

	private static final Element pcName = new Element(PC_ATTR);

	private static final String IMAGE_ATTR = "image";
	
	private Document data;

	/**
	 * Creates a new DOMFrysk using the DOM contained in data
	 * 
	 * @param data
	 *            The Document to use as the source window DOM
	 */
	public DOMFrysk(Document data) {
		this.data = data;
		this.data.getRootElement().setText("Frysk JDOM");
		this.data.getRootElement().addContent(pcName);
	}

	/**
	 * adds an image element to the DOM
	 * 
	 * @param image_name =
	 *            the name of the image to be added
	 * @param CCPATH =
	 *            the CCPATH associated with this image
	 * @param source_path =
	 *            the path to the source of this image
	 * @return
	 */
	public boolean addImage(String image_name, String CCPATH, String source_path) {
		// Make sure this image name is not already there before adding
	
		if (checkImageDup(image_name))
			return false;
		Element imageNameElement = new Element(IMAGE_ATTR);
		imageNameElement.setAttribute(DOMSource.FILENAME_ATTR, image_name);
		imageNameElement.setAttribute(CCPATH_ATTR, CCPATH);
		this.data.getRootElement().addContent(imageNameElement);
		return true;
	}

	/**
	 * Add the PID to the DOM
	 * 
	 * @param an int containing the PID
	 */
	
	public boolean addPID(int pid) {
		// Make sure there is not a PID already there before adding
		if (this.data.getRootElement().getChild(PID_ATTR) != null)
			return false;
		this.data.getRootElement().addContent(pidValue);
		this.data.getRootElement().getChild(PID_ATTR).setAttribute("value",
				Integer.toString(pid));
		return true;
	}
	/**
	 * checkImageDup - check to see if there is a duplicate image name
	 * 
	 * @param String image_name =
	 *            name of the image to check for
	 */
	
	public boolean checkImageDup(String image) {
		Iterator i = this.data.getRootElement().getChildren().iterator();
		while (i.hasNext()) {
			Element elem = (Element) i.next(); 
			if (elem.getQualifiedName().equals(IMAGE_ATTR)) {
				if (elem.getAttributeValue(DOMSource.FILENAME_ATTR)
						.equals(image))
					return true;
			}
		}
		return false;
	} 

	/**
	 * Retrieves all the images contained in the DOM as an iterator
	 * 
	 * @return
	 */
	public Iterator getImages() {
		Iterator i = this.data.getRootElement().getChildren().iterator();
		Vector v = new Vector();

		while (i.hasNext()) {
			Element elem = (Element) i.next();
			v.add(new DOMImage(elem));
		}

		return v.iterator();
	}

	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is
	 * found returns null
	 * 
	 * @param name
	 *            The name of the image to look for
	 * @return The DOMImage corresponding to the element, or null if no such
	 *         element exists
	 */
	public DOMImage getImage(String name) {
		Iterator i = this.data.getRootElement().getChildren().iterator();

		while (i.hasNext()) {
			Element elem = (Element) i.next();
			if (elem.getQualifiedName().equals(IMAGE_ATTR)) {
				if (elem.getAttributeValue(DOMSource.FILENAME_ATTR)
						.equals(name))
					return new DOMImage(elem);
			}
		}

		return null;
	}

	/**
	 * @return The PID of the process that this DOM represents
	 */
	public int getPID() {
		return Integer.parseInt(this.data.getRootElement().getChild(
				PID_ATTR).getAttribute(value).getValue());
	}

	/**
	 * @return The root element of the DOM
	 */
	protected Element getElement() {
		return this.data.getRootElement();
	}

	/**
	 * 
	 * @return BigInteger program counter
	 */
	public BigInteger getPC() {
		BigInteger bInt = new BigInteger(this.data.getRootElement().getChild(
				PC_ATTR).getAttribute(value).getValue());
		return bInt;
	}

	/**
	 * Set the PC counter value in the DOM
	 */
	public void setPC(BigInteger pc) {
		this.data.getRootElement().getChild(PC_ATTR).setAttribute(value,
				pc.toString());
	}

	/**
	 * Return the DOMFrysk document
	 */
	public Document getDOMFrysk() {
		return this.data;
	}

}
