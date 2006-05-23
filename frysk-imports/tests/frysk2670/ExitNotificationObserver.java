package frysk.gui.monitor.observers;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.TaskAction;
import frysk.proc.Action;
import frysk.proc.Task;


public class ExitNotificationObserver extends TaskTerminatingObserver {

	public ExitNotificationObserver() {
		super();
		this.setName("Exit Notifiction Observer");
		this.setToolTip("Notify the user that a task is about to exit\n" +
				"and provide them the option to stop or resume the process");
		
		frysk.gui.monitor.actions.TaskAction myAction = new TaskAction(){
			public void execute(Task task) {
				if(DialogManager.showQueryDialog("Task ["+task+"] is about to exit.\n Would you like to block it")){
					setReturnAction(Action.BLOCK);
				}else{
					setReturnAction(Action.CONTINUE);
				}
			}
		
			public GuiObject getCopy() {
				return null;
			}
		
			public boolean setArgument(String argument) {
				return true;
			}
		
			public String getArgument() {
				return null;
			}
		
			public ObservableLinkedList getArgumentCompletionList() {
				return null;
			}			
		};
		myAction.dontSaveObject();
		
		this.taskActionPoint.addAction(myAction);
		
	}
	
	public ExitNotificationObserver(ExitNotificationObserver other) {
		super(other);
		
		frysk.gui.monitor.actions.TaskAction myAction = new TaskAction(){
			public void execute(Task task) {
				if(DialogManager.showQueryDialog("Task ["+task+"] is about to exit.\n Would you like to block it")){
					setReturnAction(Action.BLOCK);
				}else{
					setReturnAction(Action.CONTINUE);
				}
			}
		
			public GuiObject getCopy() {
				return null;
			}
		
			public boolean setArgument(String argument) {
				return true;
			}
		
			public String getArgument() {
				return null;
			}
		
			public ObservableLinkedList getArgumentCompletionList() {
				return null;
			}			
		};
		myAction.dontSaveObject();
		
		this.taskActionPoint.addAction(myAction);
	}
	
	public GuiObject getCopy(){
		return new ExitNotificationObserver(this);
	}
	
}
