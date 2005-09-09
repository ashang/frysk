package frysk.gui.srcwin;

import org.gnu.gdk.Color;

public class ColorConverter {
	public static String colorToHexString(Color c){
		String s = "#";
		
		int[] parts = {c.getRed(), c.getGreen(), c.getBlue()};
		
		for(int i = 0; i < parts.length; i++){
			String tmp = Integer.toHexString(parts[i]);
			
			while(tmp.length() < 4)
				tmp = "0"+tmp;
			
			s += tmp;
		}
		
		return s;
	}
}
