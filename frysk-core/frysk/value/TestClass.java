package frysk.value;

import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.junit.TestCase;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtSignal;

public class TestClass extends TestCase {

    private CompositeType getType(String program, String variableName) {
	Task task = (new DaemonBlockedAtSignal(program)).getMainTask();
	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);

	ObjectDeclarationSearchEngine objectDeclarationSearchEngine = new ObjectDeclarationSearchEngine(
		frame);

	Variable variable = (Variable) objectDeclarationSearchEngine
		.getVariable(variableName);

	assertNotNull("Variable found", variable);

	Type type = variable.getType(frame.getTask().getISA());
	CompositeType compType = null;

	try {
	    compType = (CompositeType) type;
	} catch (ClassCastException e) {
	    fail("Not a composite type");
	}

	return compType;
    }

    public void testSimpleClass() {
	CompositeType type = getType("funit-simple-class", "simple");

	assertEquals("Variable is a class", type.getPrefix(), "class");
    }

    public void testComplexClass() {
	CompositeType type = getType("funit-complex-class", "complex");

	assertEquals("Variable is a class", "class", type.getPrefix());
    }

    public void testSimpleStruct() {
	CompositeType type = getType("funit-simple-struct", "simple");

	assertEquals("Variable is a struct", "struct", type.getPrefix());
    }

    public void testComplexStruct() {
	CompositeType type = getType("funit-complex-struct", "complex");

	// Check the type of the variable, new compiler says struct, old
	// compiler says class.
	if (unresolvedCompilerNoSupportForAT_CLASS())
	    assertEquals("Variable is considered a class", "class", type
		    .getPrefix());
	else
	    assertEquals("Variable is a struct", "struct", type.getPrefix());

    }

    public void testInheritedStruct() {
	CompositeType type = getType("funit-inherited-struct", "der");

	// Check the type of the variable, new compiler says struct, old
	// compiler says class.
	if (unresolvedCompilerNoSupportForAT_CLASS())
	    assertEquals("Variable is considered a class", "class", type
		    .getPrefix());
	else
	    assertEquals("Variable is a struct", "struct", type.getPrefix());
    }

}
