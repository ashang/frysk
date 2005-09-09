package frysk.gui.monitor;

import frysk.proc.Task;

/**
 * Used to store a pointer to the Task object, and and any data that is relates
 * to the process but is gui specific. Used to pass data to ActionPool Actions.
 * Actions also manipulate data stored in here to keep it up to date.
 */
public class TaskData {

	private Task task;

	TaskData(Task task) {
		this.task = task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Task getTask() {
		return task;
	}

}
