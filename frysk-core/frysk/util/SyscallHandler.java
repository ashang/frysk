package frysk.util;
import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;

public interface SyscallHandler
{
	void handle(Task task, SyscallEventInfo syscall, int when);
}
