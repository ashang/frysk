package frysk.sys;

import junit.framework.TestCase;



public class TestPtraceByteBuffer extends TestCase {
	
	private int pid;
	
	public void testPeek() {
		pid = TestLib.forkIt();
		assertTrue(pid > 0);
		Ptrace.attach(pid);
		System.out.println("JAVA: Finished attach");
		int temp = TestLib.waitIt(pid);
		assertEquals("Return from waitpid()", temp, pid);
		
	}

}
