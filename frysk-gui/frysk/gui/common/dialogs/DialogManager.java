package frysk.gui.common.dialogs;

/**
 * A class with public static methods for showing dialogs
 * and getting user responces
 * */
public class DialogManager {


	/**
	 * Pops up a WarnDialog with the given message
	 * @param message the message to be shown to the user
	 * */
	public static void showWarnDialog(String message){
		WarnDialog myDialog = new WarnDialog(message);
		myDialog.showAll();
		myDialog.run();
	}

}
