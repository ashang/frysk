// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.ftrace;

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcTasksObserver;
import frysk.proc.ProcTasksAction;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.isa.syscalls.Syscall;
import frysk.isa.signals.Signal;
import inua.util.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import frysk.proc.TaskAttachedObserverXXX;
import frysk.debuginfo.PrintStackOptions;

public class Ftrace {
    private final PrintStackOptions stackPrintOptions;
    
    public Ftrace(PrintStackOptions stackPrintOptions) {
	this.stackPrintOptions = stackPrintOptions;
	reporter = new Reporter(new PrintWriter(System.out), stackPrintOptions);
    }

    // Where to send output.
    Reporter reporter;

    // True if we're tracing children as well.
    boolean traceChildren = false;

    // True if we're tracing mmaps/unmaps.
    boolean traceMmapUnmap = false;

    HashMap syscallCache = new HashMap();

    // The number of processes we're tracing.
    int numProcesses;

    /**
     * Controller has to be implemented externally.  Each time a
     * mapping changes, it is called for consulation and has a chance
     * to change working set of Ftrace via provided Driver interface.
     */
    public static interface Controller {
	/**
	 * New library FILE was mapped in task TASK.  Use DRIVER to tell
	 * ltrace what to do.
	 */
	void fileMapped(frysk.proc.Task task, ObjectFile file, Driver driver);
    }

    /**
     * External entity implementing this interface is called out each
     * time an entry point is hit.  It can decide whether the stack
     * trace should be generated or not.
     * XXX: Ideally, this would also operate on tracepoints.
     */
    public static interface StackTracedSymbolsProvider {
	boolean shouldStackTraceOnSymbol(Symbol symbol);
    }

    /**
     * External entity implementing this interface is called out for
     * each new process, and should construct set of syscalls to trace
     * and stack trace on.
     */
    public static interface TracedSyscallProvider {
	/** Answers Map&lt;Syscall, Boolean&gt;, where the boolean
    	    value is whether to stack trace on given syscall. */
	Map computeSyscallWorkingSet(Task task);
    }

    /**
     * External entity implementing this interface is called out for
     * each new process, and should construct set of signals to trace
     * and stack trace on.
     */
    public static interface TracedSignalProvider {
	/** Answers Map&lt;String, Boolean&gt;, where the boolean
    	    value is whether to stack trace on given signal. */
	Map computeSignalWorkingSet(Task task);
    }

    /**
     * Driver implementation is placed here in Ftrace, and handed over
     * via this interface to allow external controller to aid which
     * tracepoints should be traced.
     */
    public static interface Driver {
	void tracePoint(Task task, TracePoint tp);
    }

    // Non-null if we're using ltrace.
    Controller ftraceController = null;
    StackTracedSymbolsProvider stackTraceSetProvider = null;
    TracedSyscallProvider tracedSyscallProvider = null;
    TracedSignalProvider tracedSignalProvider = null;

    public void setTraceChildren ()
    {
	traceChildren = true;
    }

    public void setTraceSyscalls (TracedSyscallProvider tracedSyscallProvider)
    {
	FtraceLogger.finest.log("syscall tracing requested");
	this.tracedSyscallProvider = tracedSyscallProvider;
    }

    public void setTraceSignals (TracedSignalProvider tracedSignalProvider)
    {
	FtraceLogger.finest.log("signal tracing requested");
	this.tracedSignalProvider = tracedSignalProvider;
    }

    public void setTraceMmaps ()
    {
	traceMmapUnmap = true;
    }

    public void setTraceFunctions (Controller ftraceController,
				   StackTracedSymbolsProvider stackTraceSetProvider)
    {
	if (ftraceController == null
	    || stackTraceSetProvider == null)
	    throw new AssertionError("ftraceController != null && stackTraceSetProvider != null");

	if (this.ftraceController == null
	    && this.stackTraceSetProvider == null) {
	    this.ftraceController = ftraceController;
	    this.stackTraceSetProvider = stackTraceSetProvider;
	}
	else
	    throw new AssertionError("FtraceController already assigned.");
    }

    public void setWriter (PrintWriter writer) {
	this.reporter = new Reporter(writer, stackPrintOptions);
    }

    private void init() {
	functionObserver = new MyFunctionObserver(reporter, stackTraceSetProvider);
    }

    public void addProc(Proc proc) {
	new ProcTasksAction(proc, tasksObserver);
    }

    public void trace (String[] command) {
	init();
	Manager.host.requestCreateAttachedProc(command, attachedObserver);
	Manager.eventLoop.run();
    }

    public void trace () {
	init();
	Manager.eventLoop.run();
    }

    private HashMap observationCounters = new HashMap();

    synchronized private void observationRequested(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	if (i == null)
	    i = new Integer(1);
	else
	    i = new Integer(i.intValue() + 1);
	observationCounters.put(task, i);
    }

    synchronized private void observationRealized(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	// must be non-null
	int j = i.intValue();
	if (j == 1) {
	    // Store a dummy into the map to detect errors.
	    observationCounters.put(task, new Object());
	    task.requestUnblock(attachedObserver);
	}
	else
	    observationCounters.put(task, new Integer(--j));
    }

    synchronized void handleTask (Task task)
    {
	Proc proc = task.getProc();

	if (tracedSyscallProvider != null) {
	    FtraceLogger.finest.log("requesting syscall observer");
	    task.requestAddSyscallsObserver(new MySyscallObserver(reporter));
	    observationRequested(task);
	    Map workingSet
		= tracedSyscallProvider.computeSyscallWorkingSet(task);
	    syscallSetForTask.put(task, workingSet);
	}

	if (tracedSignalProvider != null) {
	    FtraceLogger.finest.log("requesting signal observer");
	    task.requestAddSignaledObserver(new MySignaledObserver());
	    observationRequested(task);
	    Map workingSet
		= tracedSignalProvider.computeSignalWorkingSet(task);
	    signalSetForTask.put(task, workingSet);
	}

	task.requestAddForkedObserver(forkedObserver);
	observationRequested(task);

	task.requestAddClonedObserver(clonedObserver);
	observationRequested(task);

	task.requestAddTerminatedObserver(new MyTerminatedObserver());
	observationRequested(task);

	if (ftraceController != null || traceMmapUnmap) {
	    MyMappingObserver o = new MyMappingObserver(ftraceController);

	    // Presumably the user would like to see mappings and
	    // unmappings as precisely as possible, and all of them.
	    // Use syscall-based observer in that case.
	    if (traceMmapUnmap)
		MappingGuard.requestAddSyscallBasedMappingObserver(task, o);
	    else
		MappingGuard.requestAddMappingObserver(task, o);

	    observationRequested(task);
	}

	new ProcRemovedObserver(proc);

	reporter.eventSingle(task, "attached " + proc.getExeFile().getSysRootedPath());
	++numProcesses;
    }

    /** Remembers working set preferences for each task.
	Map&lt;Task, TracePointWorkingSet&gt; */
    private final HashMap driversForTask = new HashMap();

    /** Remembers traced syscall set for each task.
        Map&lt;Task, Map&lt;Syscall, Boolean&gt;&gt; */
    private final HashMap syscallSetForTask = new HashMap();

    /** Remembers traced signal set for each task.
        Map&lt;Task, Map&lt;String, Boolean&gt;&gt; */
    private final HashMap signalSetForTask = new HashMap();

    private class TracePointWorkingSet
	implements Driver
    {
	private Set tracePoints = new HashSet();

	public void tracePoint(Task task, TracePoint tp)
	{
	    FtraceLogger.info.log("Request for tracing `" + tp.symbol.name + "'");
	    tracePoints.add(tp);
	}

	public void populateBreakpoints(Task task, MemoryMapping mapping, MemoryMapping.Part part)
	{
	    Set request = new HashSet();
	    for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
		TracePoint tp = (TracePoint)it.next();
		if (tp.offset >= part.offset
		    && tp.offset < part.offset + part.addressHigh - part.addressLow) {
		    FtraceLogger.finest.log(
			"Will trace `" + tp.symbol.name + "', "
			+ "address=0x" + Long.toHexString(tp.address) + "; "
			+ "offset=0x" + Long.toHexString(tp.offset) + "; "
			+ "part at=0x" + Long.toHexString(part.addressLow)
			+ ".." + Long.toHexString(part.addressHigh) + "; "
			+ "part off=0x" + Long.toHexString(part.offset) + ";");

		    long actualAddress = tp.offset - part.offset + part.addressLow;
		    TracePoint.Instance tpi = new TracePoint.Instance(tp, actualAddress);
		    FtraceLogger.info.log(
			"Will trace `" + tpi.tracePoint.symbol.name
			+ "' at 0x" + Long.toHexString(tpi.address));

		    request.add(tpi);
		}
	    }
	    if (!request.isEmpty())
		Ltrace.requestAddFunctionObserver(task, functionObserver, request);
	}

	public void evacuateBreakpoints(Task task, MemoryMapping mapping, MemoryMapping.Part part)
	{
	    Set request = new HashSet();
	    for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
		TracePoint tp = (TracePoint)it.next();
		if (tp.offset >= part.offset
		    && tp.offset < part.offset + part.addressHigh - part.addressLow) {

		    long actualAddress = tp.offset - part.offset + part.addressLow;
		    TracePoint.Instance tpi = new TracePoint.Instance(tp, actualAddress);
		    FtraceLogger.info.log(
			"Stopping tracing of `" + tpi.tracePoint.symbol.name
			+ "' at 0x" + Long.toHexString(tpi.address));

		    request.add(tpi);
		}
	    }
	    if (!request.isEmpty())
		Ltrace.requestDeleteFunctionObserver(task, functionObserver, request);
	}
    }

    private ProcTasksObserver tasksObserver = new ProcTasksObserver() {
	    public void existingTask(Task task) {
		handleTask(task);
		if (task == task.getProc().getMainTask()) {
		    // Unblock forked and cloned observer, which
		    // blocks main task after the fork or clone, to
		    // give us a chance to pick it up.
		    task.requestUnblock(forkedObserver);
		    task.requestUnblock(clonedObserver);
		}
	    }
	    public void taskAdded(Task task) {
		handleTask(task);
	    }
	    public void taskRemoved(Task task) {
	    }
	    public void addedTo (Object observable)	{}
	    public void addFailed (Object observable, Throwable arg1) {}
	    public void deletedFrom (Object observable) {}
	};


    /**
     * An observer to stop the eventloop when the traced process
     * exits.
     */
    private class ProcRemovedObserver
	implements TaskObserver.Terminated
    {
	ProcRemovedObserver (Proc proc) {
	    proc.getMainTask().requestAddTerminatedObserver(this);
	}

	public void addedTo (Object observable)	{}
	public void addFailed (Object observable, Throwable arg1) {}
	public void deletedFrom (Object observable) {}

	public Action updateTerminated(Task task, Signal signal, int status) {
	    synchronized (Ftrace.this) {
		--numProcesses;
		if (numProcesses == 0)
		    Manager.eventLoop.requestStop();
	    }
	    return Action.CONTINUE;
	}
    }

    /**
     * An observer that sets up things once frysk has set up the requested
     * proc and attached to it.
     */
    class MyAttachedObserver implements TaskAttachedObserverXXX {
	private Set procs = new HashSet();
	public synchronized Action updateAttached (Task task)
	{
	    Proc proc = task.getProc();
	    if (!procs.contains(proc)) {
		procs.add(proc);
		addProc(task.getProc());
	    }

	    // To make sure all the observers are attached before the task
	    // continues, the attachedObserver blocks the task.  As each of
	    // the observers is addedTo, they call observationRealized.
	    // Task is unblocked when all requested observations are
	    // realized.
	    return Action.BLOCK;
	}

	public void addedTo (Object observable) {}
	public void deletedFrom (Object observable) {}
	public void addFailed (Object observable, Throwable w) {
	    throw new RuntimeException("Failed to attach to created proc", w);
	}
    }
    MyAttachedObserver attachedObserver = new MyAttachedObserver();

    /**
     * The syscallObserver added to the traced proc.
     */
    private class MySyscallObserver implements TaskObserver.Syscalls {
	private final Reporter reporter;
	private Syscall syscallCache = null;

	MySyscallObserver(Reporter reporter) {
	    this.reporter = reporter;
	}

	public Action updateSyscallEnter(Task task, Syscall syscall) {
	    syscallCache = syscall;

	    String name = syscall.getName();
	    Map syscallWorkingSet = (Map)syscallSetForTask.get(task);
	    Boolean stackTrace = (Boolean)syscallWorkingSet.get(syscall);
	    if (stackTrace == null)
		return Action.CONTINUE;

	    if (syscall.isNoReturn())
		reporter.eventSingle(task, "syscall " + name,
				     syscall.extractCallArguments(task));
	    else
		reporter.eventEntry(task, syscall, "syscall", name,
				    syscall.extractCallArguments(task));

	    if (stackTrace.booleanValue())
		reporter.generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public Action updateSyscallExit (Task task)
	{
	    Syscall syscall = syscallCache;
	    if (((Map)syscallSetForTask.get(task)).get(syscall) == null)
		return Action.CONTINUE;

	    String name = syscall.getName();

	    reporter.eventLeave(task, syscall,
				"syscall leave", name,
				syscall.extractReturnValue(task));

	    syscallCache = null;
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable)
	{
	    FtraceLogger.finest.log("syscall observer realized");
	    Task task = (Task) observable;
	    observationRealized(task);
	}

	public void addFailed (Object observable, Throwable w)
	{
	    throw new RuntimeException("Failed to add a Systemcall observer to the process", w);
	}

	public void deletedFrom (Object observable) { }
    }

    class ForkCloneObserverBase
	implements TaskObserver
    {
	public void addFailed (Object observable, Throwable w)
	{
	}

	public void addedTo (Object observable)
	{
	    Task task = (Task) observable;
	    observationRealized(task);
	}

	public void deletedFrom (Object observable)
	{
	}
    }

    class MyForkedObserver
	extends ForkCloneObserverBase
	implements TaskObserver.Forked
    {
	public Action updateForkedOffspring (Task parent, Task offspring)
	{
	    if (offspring != offspring.getProc().getMainTask())
		// If this assertion doesn't hold, probably no
		// biggie, but you have to unblock the right tasks
		// in existingTask.
		throw new AssertionError("assert offspring == offspring.getProc().getMainTask()");

	    if(traceChildren) {
		addProc(offspring.getProc());

		// Will be unblocked when existingTask picks it up,
		// otherwise we'd miss on events.
		return Action.BLOCK;
	    }

	    return Action.CONTINUE;
	}

	public Action updateForkedParent (Task parent, Task offspring)
	{
	    return Action.CONTINUE;
	}
    }
    TaskObserver.Forked forkedObserver = new MyForkedObserver();

    class MyClonedObserver
	extends ForkCloneObserverBase
	implements TaskObserver.Cloned
    {
	public Action updateClonedOffspring (Task parent, Task offspring)
	{
	    return Action.CONTINUE;
	}

	public Action updateClonedParent (Task parent, Task offspring)
	{
	    return Action.CONTINUE;
	}
    }
    TaskObserver.Cloned clonedObserver = new MyClonedObserver();

    class MyTerminatedObserver implements TaskObserver.Terminated {
	public Action updateTerminated (Task task, Signal signal, int value) {
	    if (signal != null)
		reporter.eventSingle(task, "killed by " + signal);
	    else
		reporter.eventSingle(task, "exited with status " + value);
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) {
	    Task task = (Task) observable;
	    observationRealized(task);
	}
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) { }
    }

    class MySignaledObserver implements TaskObserver.Signaled {
	public Action updateSignaled(Task task, Signal signal) {
	    FtraceLogger.finest.log("signal hit " + signal);
	    String name = signal.getName();
	    Map signalWorkingSet = (Map)signalSetForTask.get(task);
	    Boolean stackTrace = (Boolean)signalWorkingSet.get(signal);
	    if (stackTrace == null)
		return Action.CONTINUE;

	    reporter.eventSingle(task, "signal " + name);

	    if (stackTrace.booleanValue())
		reporter.generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) {
	    FtraceLogger.finest.log("signal observer realized for " + observable);
	    Task task = (Task) observable;
	    observationRealized(task);
	}
	public void deletedFrom (Object observable) {
	    FtraceLogger.finest.log("signal observer deleted from " + observable);
	}
	public void addFailed (Object observable, Throwable w) {
	    FtraceLogger.finest.log("signal observer failure for "
				    + observable + " with " + w);
	}
    }

    class MyMappingObserver
	implements MappingObserver
    {
	private final Controller tracingController;

	MyMappingObserver(Controller controller) {
	    this.tracingController = controller;
	}

	public Action updateMappedFile(frysk.proc.Task task, MemoryMapping mapping)
	{
	    if (traceMmapUnmap)
		reporter.eventSingle(task, "map " + mapping.path);

	    if (this.tracingController == null)
		return Action.CONTINUE;

	    java.io.File path = mapping.path;
	    if (path.getPath().equals("/SYSV00000000 (deleted)")) {
		// This is most probably artificial name of SYSV
		// shared memory "file".
		return Action.CONTINUE;
	    }
	    ObjectFile objf = ObjectFile.buildFromFile(path);
	    if (objf == null)
		return Action.CONTINUE;

	    TracePointWorkingSet driver = new TracePointWorkingSet();
	    Map drivers = (Map)driversForTask.get(task);
	    if (drivers == null) {
		drivers = new HashMap();
		driversForTask.put(task, drivers);
	    }
	    drivers.put(mapping.path, driver);
	    this.tracingController.fileMapped(task, objf, driver);

	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	public Action updateUnmappedFile(frysk.proc.Task task, MemoryMapping mapping)
	{
	    if (traceMmapUnmap)
		reporter.eventSingle(task, "unmap " + mapping.path);
	    return Action.CONTINUE;
	}

	private TracePointWorkingSet getDriver(Task task, MemoryMapping mapping) {
	    // This has to be non-null.
	    Map drivers = (Map)driversForTask.get(task);
	    // This can be null for non-elf files.  We don't care about
	    // these.
	    return (TracePointWorkingSet)drivers.get(mapping.path);
	}

	private boolean processMappedPart(MemoryMapping.Part part) {
	    // Ignore non-executable mappings.
	    // Ignore mappedPart messages if we don't trace symbols.
	    return part.permExecute && this.tracingController != null;
	}

	public Action updateMappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part)
	{
	    if (!processMappedPart(part))
		return Action.CONTINUE;

	    TracePointWorkingSet driver = getDriver(task, mapping);
	    if (driver == null)
		return Action.CONTINUE;

	    // At this point we already know client preferences
	    // regarding which tracepoints should be in working set.
	    // Just apply this knowledge on new mapped part: all working
	    // set elements that fall into this part should be observed.
	    driver.populateBreakpoints(task, mapping, part);

	    task.requestUnblock(this);
	    return Action.BLOCK;
    	}

	public Action updateUnmappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part)
	{
	    if (!processMappedPart(part))
		return Action.CONTINUE;

	    TracePointWorkingSet driver = getDriver(task, mapping);
	    if (driver == null)
		return Action.CONTINUE;

	    driver.evacuateBreakpoints(task, mapping, part);

	    task.requestUnblock(this);
	    return Action.BLOCK;
    	}

	public void addedTo (Object observable) {
	    Task task = (Task) observable;
	    observationRealized(task);
	}
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) { }
    }


    private class MyFunctionObserver
	implements FunctionObserver
    {
	// Reporting tool.
	private Reporter reporter;
	private StackTracedSymbolsProvider stackTraceSetProvider;

	MyFunctionObserver(Reporter reporter, StackTracedSymbolsProvider stackTraceSetProvider) {
	    this.reporter = reporter;
	    this.stackTraceSetProvider = stackTraceSetProvider;
	}

	public synchronized Action funcallEnter(Task task, Symbol symbol, Object[] args)
	{
	    String symbolName = symbol.name;
	    String callerLibrary = symbol.getParent().getSoname();
	    String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	    reporter.eventEntry(task, symbol, "call", eventName, args);

	    // If this systsysem call is in the stack tracing HashSet,
	    // get a stack trace before continuing on.
	    if (stackTraceSetProvider.shouldStackTraceOnSymbol(symbol))
		reporter.generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public synchronized Action funcallLeave(Task task, Symbol symbol, Object retVal)
	{
	    reporter.eventLeave(task, symbol, "leave", symbol.name, retVal);
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) { }
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) { }
    }

    public MyFunctionObserver functionObserver = null;
}
