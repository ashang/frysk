// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import frysk.Config;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.testbed.*;

import java.util.*;
import java.util.regex.*;

/**
 * This is a test for basic ltrace capabilities.
 */
public class TestLtrace
    extends TestLib
{
    class DummyFunctionObserver
	implements FunctionObserver
    {
	public Action funcallEnter(Task task, Symbol symbol, Object[] args) {
	    return Action.CONTINUE;
	}
	public Action funcallLeave(Task task, Symbol symbol, Object retVal) {
	    return Action.CONTINUE;
	}
	public void addedTo (Object observable) { }
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) {}
    }

    abstract class ObserverCreator {
	abstract FunctionObserver createObserver();
	public boolean shouldAcceptMapping(Task task, String name) {
	    return task.getProc().getExe().equals(name);
	}
	public boolean acceptTracepoint(Task task, TracePoint tp) {
	    return tp.origin == TracePointOrigin.PLT;
	}
    }

    class GenericMappingObserver
	extends TestMappingGuard.DummyMappingObserver
    {
	final ObserverCreator creator;

	public GenericMappingObserver(ObserverCreator creator) {
	    this.creator = creator;
	}

	public Action updateMappedFile(final Task task, MemoryMapping mapping) {
	    block: {
		if (!creator.shouldAcceptMapping(task, mapping.path.getPath()))
		    break block;

		try {
		    ObjectFile objf = ObjectFile.buildFromFile(mapping.path);
		    if (objf == null)
			throw new AssertionError("NULL objf for a file whose name matches main binary?");

		    final HashSet tps = new HashSet();
		    objf.eachTracePoint(new ObjectFile.TracePointIterator() {
			    public void tracePoint(TracePoint tp) {
				if (creator.acceptTracepoint(task, tp))
				    tps.add(tp);
			    }
			}, TracePointOrigin.PLT);
		    objf.eachTracePoint(new ObjectFile.TracePointIterator() {
			    public void tracePoint(TracePoint tp) {
				if (creator.acceptTracepoint(task, tp))
				    tps.add(tp);
			    }
			}, TracePointOrigin.SYMTAB);
		    objf.eachTracePoint(new ObjectFile.TracePointIterator() {
			    public void tracePoint(TracePoint tp) {
				if (creator.acceptTracepoint(task, tp))
				    tps.add(tp);
			    }
			}, TracePointOrigin.DYNAMIC);

		    if (!tps.isEmpty()) {
			FunctionObserver fo = creator.createObserver();
			Ltrace.requestAddFunctionObserver(task, fo, tps);
			task.requestUnblock(this);
			return Action.BLOCK;
		    }
		}
		catch (lib.dwfl.ElfException ee) {
		    ee.printStackTrace();
		}
	    }

	    return super.updateMappedFile(task, mapping);
	}
    }

    public void testCallRecorded()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	final ArrayList events = new ArrayList();

	class MyFunctionObserver1
	    extends DummyFunctionObserver
	{
	    public Action funcallEnter(Task task, Symbol symbol, Object[] args) {
		events.add("enter " + symbol.name);
		return Action.CONTINUE;
	    }
	    public Action funcallLeave(Task task, Symbol symbol, Object retVal) {
		events.add("leave " + symbol.name);
		return Action.CONTINUE;
	    }
	}

	GenericMappingObserver mappingObserver
	    = new GenericMappingObserver(new ObserverCreator() {
		    public FunctionObserver createObserver() {
			return new MyFunctionObserver1();
		    }
	    });

	String[] cmd = {Config.getPkgLibFile("funit-syscalls").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Proc proc = task.getProc();
	int pid = proc.getPid();

	MappingGuard.requestAddMappingObserver(task, mappingObserver);
	assertRunUntilStop("add mapping observer");

	new StopEventLoopWhenProcRemoved(pid);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	String[] expectedEvents = {
	    "enter __libc_start_main",
	    "enter open(64)?",
	    "leave open(64)?",
	    "enter close",
	    "leave close"
	};
	for (int i = 0; i < expectedEvents.length; ++i) {
	    if (i >= events.size())
		break;
	    String event = (String)events.get(i);
	    assertTrue("event `" + event + "' detected",
		       Pattern.matches(expectedEvents[i], event));
	}
	assertEquals("number of recorded events", expectedEvents.length, events.size());
    }

    public void testArgumentsCorrect1()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	final Set registeredSymbols = new HashSet();
	final LinkedList expectedEvents = new LinkedList();
	final LinkedList expectedReturns = new LinkedList();

	class ExpectedEvent {
	    String name;
	    long[] arguments;
	    long retval;

	    ExpectedEvent(String name, long[] arguments, long retval) {
		this.name = name;
		this.retval = retval;
		this.arguments = arguments;
		registeredSymbols.add(name);
		expectedEvents.addLast(this);
	    }
	}

	new ExpectedEvent("trace_me_1", new long[]{3, 5, 7, 11, 13, 17}, 56);
	new ExpectedEvent("trace_me_2", new long[]{3, 5, 7, 11, 13, 17}, 56);

	class MyFunctionObserver2
	    extends DummyFunctionObserver
	{
	    public Action funcallEnter(Task task, Symbol symbol, Object[] args) {
		ExpectedEvent ee = (ExpectedEvent)expectedEvents.removeFirst();
		assertEquals("enter function name", ee.name, symbol.name);
		for (int i = 0; i < ee.arguments.length; ++i) {
		    assertTrue("argument #" + i + " of function " + ee.name + " is a number",
			       args[i] instanceof Number);
		    // If ^^ this fails, ltrace probably grew brans to
		    // answer all kinds of objects, not just sixtuples
		    // of integers.

		    assertEquals("argument #" + i + " of function " + ee.name,
				 ee.arguments[i], ((Number)args[i]).longValue());
		}
		expectedReturns.add(ee);
		return Action.CONTINUE;
	    }

	    public Action funcallLeave(Task task, Symbol symbol, Object retVal) {
		ExpectedEvent ee = (ExpectedEvent)expectedReturns.removeLast();
		assertEquals("leave function name", ee.name, symbol.name);
		assertTrue("return value of function " + ee.name + " is a number",
			   retVal instanceof Number);
		assertEquals("return value of function " + ee.name,
			     ee.retval, ((Number)retVal).longValue());
		return Action.CONTINUE;
	    }
	}

	String[] cmd = {Config.getPkgLibFile("funit-calls").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Proc proc = task.getProc();
	int pid = proc.getPid();

	GenericMappingObserver mappingObserver
	    = new GenericMappingObserver(new ObserverCreator() {
		    public FunctionObserver createObserver() {
			return new MyFunctionObserver2();
		    }
		    public boolean acceptTracepoint(Task task, TracePoint tp) {
			return registeredSymbols.contains(tp.symbol.name);
		    }
	    });

	MappingGuard.requestAddMappingObserver(task, mappingObserver);
	assertRunUntilStop("add mapping observer");

	new StopEventLoopWhenProcRemoved(pid);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	assertEquals("number of unprocessed expects", 0, expectedEvents.size());
	assertEquals("number of unprocessed returns", 0, expectedReturns.size());
    }

    public void testTracingAlias()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	final HashSet enterAliases = new HashSet();
	final HashSet leaveAliases = new HashSet();

	class MyFunctionObserver3
	    extends DummyFunctionObserver
	{
	    String name;

	    public MyFunctionObserver3 (String name) {
		this.name = name;
	    }

	    private void addAliases(Symbol symbol, HashSet aliases) {
		aliases.add(symbol.name);
		if (symbol.aliases != null)
		    for (int i = 0; i < symbol.aliases.size(); ++i)
			aliases.add(symbol.aliases.get(i));
	    }

	    public Action funcallEnter(Task task, Symbol symbol, Object[] args) {
		assertTrue("enter symbol matches name `" + this.name + "'",
			   symbol.hasName(this.name));
		addAliases(symbol, enterAliases);
		return Action.CONTINUE;
	    }
	    public Action funcallLeave(Task task, Symbol symbol, Object retVal) {
		assertTrue("leave symbol matches name `" + this.name + "'",
			   symbol.hasName(this.name));
		addAliases(symbol, leaveAliases);
		return Action.CONTINUE;
	    }
	}

	class MyObserverCreator3
	    extends ObserverCreator
	{
	    int found = 0;
	    final String ownName;
	    final String aliasName;

	    public MyObserverCreator3(String aliasName, String ownName) {
		this.ownName = ownName;
		this.aliasName = aliasName;
	    }

	    public FunctionObserver createObserver() {
		return new MyFunctionObserver3(ownName);
	    }

	    public boolean acceptTracepoint(Task task, TracePoint tp) {
		if (tp.symbol.hasName(aliasName)) {
		    found++;
		    return true;
		}
		else
		    return false;
	    }
	}

	String[] cmd = {Config.getPkgLibFile("funit-calls").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Proc proc = task.getProc();
	int pid = proc.getPid();

	String symbols[] = {"fun1", "alias1", "alias2"};
	MyObserverCreator3 observerCreator = new MyObserverCreator3("alias2", "fun1");
	MappingGuard.requestAddMappingObserver(task, new GenericMappingObserver(observerCreator));
	assertRunUntilStop("add mapping observer");

	new StopEventLoopWhenProcRemoved(pid);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	assertEquals("number of tracepoints requested for tracing", 1, observerCreator.found);
	for (int i = 0; i < symbols.length; ++i) {
	    assertTrue("saw enter of symbol " + symbols[i], enterAliases.contains(symbols[i]));
	    assertTrue("saw leave of symbol " + symbols[i], leaveAliases.contains(symbols[i]));
	}
	assertEquals("number of enter aliases seen", symbols.length, enterAliases.size());
	assertEquals("number of leave aliases seen", symbols.length, leaveAliases.size());
    }

    /*
    public void testMultipleObservers()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	String[] cmd = {Config.getPkgLibFile("funit-calls").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Proc proc = task.getProc();
	int pid = proc.getPid();

	int N = 10;
	LtraceController controller = new MyController4("trace_me_1");
	MyObserver5[] observers = new MyObserver5[N];
	for (int i = 0; i < N; i++) {
	    observers[i] = new MyObserver5();
	    Ltrace.requestAddFunctionObserver(task, observers[i], controller);
	}
	assertRunUntilStop("add function observers");
	for (int i = 0; i < N; i++)
	    assertTrue("observer #" + i + " added", observers[i].added);

	new StopEventLoopWhenProcRemoved(pid);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	for (int i = 0; i < N; i++) {
	    assertEquals("observer #" + i + " number of enter hits", 1, observers[i].enter);
	    assertEquals("observer #" + i + " number of leave hits", 1, observers[i].leave);
	}
    }

    public void testMultipleControlers()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	String[] cmd = {Config.getPkgLibFile("funit-calls").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Proc proc = task.getProc();
	int pid = proc.getPid();

	int N = 10;
	String[] symbols = {"trace_me_1", "trace_me_2"};
	MyController4[] controllers = new MyController4[symbols.length];
	MyObserver5[][] observers = new MyObserver5[symbols.length][N];
	for (int j = 0; j < symbols.length; j++) {
	    controllers[j] = new MyController4(symbols[j]);
	    for (int i = 0; i < N; i++) {
		observers[j][i] = new MyObserver5();
		Ltrace.requestAddFunctionObserver(task, observers[j][i], controllers[j]);
	    }
	}
	assertRunUntilStop("add function observers");
	for (int j = 0; j < symbols.length; j++)
	    for (int i = 0; i < N; i++)
		assertTrue("observer #" + i + " added", observers[j][i].added);

	new StopEventLoopWhenProcRemoved(pid);
	child.requestRemoveBlock();
	assertRunUntilStop("run child until exit");

	for (int j = 0; j < symbols.length; j++)
	    assertEquals("controller #" + j + " entry points", 1, controllers[j].found);

	for (int j = 0; j < symbols.length; j++)
	    for (int i = 0; i < N; i++) {
		assertEquals("observer #" + i + " number of enter hits", 1, observers[j][i].enter);
		assertEquals("observer #" + i + " number of leave hits", 1, observers[j][i].leave);
	    }
    }

    public void tearDown()
    {
    }

    class MyController4
	implements LtraceController
    {
	final String name;
	int found = 0;

	MyController4(String name) {
	    this.name = name;
	}

	public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver)
	{
	    if (!task.getProc().getExe().equals(objf.getFilename().getPath()))
		return;

	    try {
		objf.eachTracePoint(new ObjectFile.TracePointIterator() {
			public void tracePoint(TracePoint tp) {
			    if (tp.symbol.hasName(MyController4.this.name)) {
				++MyController4.this.found;
				driver.tracePoint(task, tp);
			    }
			}
		    }, TracePointOrigin.SYMTAB);
	    }
	    catch (lib.dwfl.ElfException ee) {
		ee.printStackTrace();
	    }
	}
    }

    class MyObserver5 extends DummyFunctionObserver {
	boolean added = false;
	int enter = 0;
	int leave = 0;
	public Action funcallEnter(Task task, Symbol symbol, Object[] args) {
	    enter++;
	    return Action.CONTINUE;
	}
	public Action funcallLeave(Task task, Symbol symbol, Object retVal) {
	    leave++;
	    return Action.CONTINUE;
	}
	public void addedTo (Object observable) {
	    super.addedTo (observable);
	    added = true;
	}
    }
    */
}
