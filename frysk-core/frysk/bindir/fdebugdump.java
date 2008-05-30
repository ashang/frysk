package frysk.bindir;

import java.io.PrintWriter;
import java.util.Iterator;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import frysk.dwfl.DwflCache;
import frysk.isa.signals.Signal;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.util.ProcRunUtil;
import frysk.util.ProcRunUtil.ProcRunObserver;
import frysk.util.ProcRunUtil.RunUtilOptions;
import gnu.classpath.tools.getopt.OptionGroup;

public class fdebugdump{

    private static PrintWriter printWriter = new PrintWriter(System.out);
    
    public static void main(String[] args) {
	RunUtilOptions options = new RunUtilOptions();
	
	OptionGroup[] customOptions = new OptionGroup[]{};
	 
	ProcRunUtil procRunUtil = new ProcRunUtil("fdebugdump", "fdebugdump <exe>", args, procRunObserver, customOptions, options);
	procRunUtil.start();
    }
    
    private static ProcRunObserver procRunObserver = new ProcRunObserver(){

	public Action updateAttached(Task task) {
	    Dwfl dwfl = DwflCache.getDwfl(task);
	    DwflModule[] dwflModules = dwfl.getModules();
	    for (int i = 0; i < dwflModules.length; i++) {
		DwflModule module = dwflModules[i];
		printWriter.println("module: " + module.getName());
		Iterator iterator = module.getCuDies().iterator();
		while (iterator.hasNext()) {
		    DwarfDie die = (DwarfDie) iterator.next();
		    printDie(die, " ");
		}
	    }
	    return Action.CONTINUE;
	}

	public void addFailed(Object observable, Throwable w) {}

	public void addedTo(Object observable) {}

	public void deletedFrom(Object observable) {}

	public Action updateForkedOffspring(Task parent, Task offspring) { return Action.CONTINUE; }
	public Action updateForkedParent(Task parent, Task offspring) { return Action.CONTINUE; }
	public Action updateExeced(Task task) { return Action.CONTINUE; }
	public Action updateClonedParent(Task task, Task clone) { return Action.CONTINUE; }
	public Action updateTerminated(Task task, Signal signal, int value) { return Action.CONTINUE; }
	public Action updateClonedOffspring(Task parent, Task offspring) { return Action.CONTINUE; }
	public void existingTask(Task task) {}
	public void taskAdded(Task task) {}
	public void taskRemoved(Task task) {}
	
    };
    
    private static void printDie(DwarfDie die, String indent){
	printWriter.println(indent + die.getTag() + " " + die.getName());
	
	die = die.getChild();
	while(die != null){
	    printDie(die, indent+ "  ");
	    die = die.getSibling();
	}
    }
}
