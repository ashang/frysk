package frysk.util;

import inua.util.PrintWriter;

import java.util.Set;

import frysk.proc.Syscall;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;

/**
 * This is a simple SyscallHandler which prints information about
 * syscalls.  It will print a partial line when a system call is
 * entered, and then print the return result (and a newline) when
 * the system call returns.
 */
public class StracePrinter implements SyscallHandler
{
	private Set tracedCalls;
	private PrintWriter writer;

	/**
	 * Create a new printer given the output writer and a set of
	 * system call names.  If tracedCalls is null, all system calls
	 * will be printed.  Otherwise a given system call will be printed
	 * only if its name appears in tracedCalls.
	 * @param writer the print writer
	 * @param tracedCalls a set of system call names, or null
	 */
	public StracePrinter(PrintWriter writer, Set/*<String>*/ tracedCalls)
	{
		this.writer = writer;
		this.tracedCalls = tracedCalls;
	}

	/**
	 * Called on system call enter and exit.
	 */
	public void handle(Task task, SyscallEventInfo syscallEventInfo, int when)
	{
		Syscall syscall = syscallEventInfo.getSyscall(task);

		if (tracedCalls == null || tracedCalls.contains(syscall.getName()))
		{
			writer.print(task.getProc().getPid() + "." + task.getTid() + " ");
			if (when == SyscallEventInfo.ENTER)
				syscall.printCall(writer, task, syscallEventInfo);
			else
				syscall.printReturn(writer, task, syscallEventInfo);
			writer.flush();
		}
	}
}
