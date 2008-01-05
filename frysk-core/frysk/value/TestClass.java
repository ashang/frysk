package frysk.value;

import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.isa.ISA;
import frysk.junit.TestCase;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;

public class TestClass extends TestCase {

    private CompositeType getType(Variable variable, ISA isa) {
	Type type = variable.getType(isa);
	CompositeType compType = null;

	try {
	    compType = (CompositeType) type;
	} catch (ClassCastException e) {
	    fail("Not a composite type");
	}

	return compType;
    }

    public void testSimpleClass() {
	String fileName = "funit-simple-class";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable("simple");

	assertNotNull("Variable found", variable);

	CompositeType type = getType(variable, frame.getTask().getISA());

	assertEquals("Variable is a class", type.getPrefix(), "class");
    }

    public void testComplexClass() {
	String fileName = "funit-complex-class";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable("complex");

	assertNotNull("Variable found", variable);

	CompositeType type = getType(variable, frame.getTask().getISA());

	if (unresolvedCompilerNoSupportForAT_CLASS())
	    return;

	assertEquals("Variable is a class", type.getPrefix(), "class");
    }

    public void testSimpleStruct() {
	String fileName = "funit-simple-struct";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable("simple");

	assertNotNull("Variable found", variable);

	CompositeType type = getType(variable, frame.getTask().getISA());

	assertEquals("Variable is a struct", type.getPrefix(), "struct");

    }

    public void testComplexStruct() {
	String fileName = "funit-complex-struct";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable("complex");

	assertNotNull("Variable found", variable);

	CompositeType type = getType(variable, frame.getTask().getISA());

	// Check the type of the variable, new compiler says struct, old
	// compiler says class.
	if (unresolvedCompilerNoSupportForAT_CLASS())
	    assertEquals("Variable is considered a class", type.getPrefix(),
		    "class");
	else
	    assertEquals("Variable is a struct", type.getPrefix(), "struct");

    }

    public void testInheritedStruct() {
	String fileName = "funit-inherited-struct";
	Task task = (new DaemonBlockedAtSignal(fileName)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable("der");

	assertNotNull("Variable found", variable);

	CompositeType type = getType(variable, frame.getTask().getISA());

	// Check the type of the variable, new compiler says struct, old
	// compiler says class.
	if (unresolvedCompilerNoSupportForAT_CLASS())
	    assertEquals("Variable is considered a class", type.getPrefix(), "class");
	else
	    assertEquals("Variable is a struct", type.getPrefix(), "struct");
    }

}
