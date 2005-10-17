/*
 * Created on Oct 13, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

public class TaskStatusWidget extends StatusWidget {

	public TaskStatusWidget(TaskData data) {
		super(data);
		this.setName("Thread ID: " + data.getTask().getTid());
	}

}
