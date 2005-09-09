package frysk.gui.monitor;

import org.gnu.gtk.Plug;
import org.gnu.javagnome.Handle;

public class EggTrayIcon extends Plug{
	EggTrayIcon(String name){
		super(egg_tray_icon_new(name));
	}
	
	EggTrayIcon(Handle screen, String name){
		super(egg_tray_icon_new_for_screen(screen, name));
	}
	
	public int sendMessage(int timeout, String message, int len){
		return egg_tray_icon_send_message(this,timeout,message,len);
	}

	public void cancelMessage(int id){
		egg_tray_icon_cancel_message (this,id);
	}
	
	public int getOrientation(){
		return egg_tray_icon_get_orientation (this);
	}
	
	native static final protected int[]  egg_tray_icon_get_type();
	native static final protected Handle egg_tray_icon_new_for_screen (Handle screen, String name);
	native static final protected Handle egg_tray_icon_new(String name);
	native static final protected int    egg_tray_icon_send_message   (EggTrayIcon icon, int timeout, String message, int len);
	native static final protected void   egg_tray_icon_cancel_message (EggTrayIcon icon, int id);
	native static final protected int    egg_tray_icon_get_orientation (EggTrayIcon icon);
}
