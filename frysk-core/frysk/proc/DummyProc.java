package frysk.proc;

public class DummyProc extends Proc {

	public DummyProc(){
		super(new DummyHost(), null, new ProcId(42));
	}
	
	public String getCommand(){
		return "Foo";
	}
	
	protected String sendrecCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String sendrecExe() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String[] sendrecCmdLine() {
		// TODO Auto-generated method stub
		return null;
	}

	void sendRefresh() {
		// TODO Auto-generated method stub

	}

	Auxv[] sendrecAuxv() {
		// TODO Auto-generated method stub
		return null;
	}

}
