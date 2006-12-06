package frysk.proc;

public class DummyProc extends Proc {

	public DummyProc(){
		super(new DummyHost(), null, new ProcId(42));
	}
	
	public String getCommand(){
		return "Foo";
	}
	
	protected String sendrecCommand() {
		return null;
	}

	protected String sendrecExe() {
		return null;
	}

	protected int sendrecUID() {
		return 0;
	}

        protected int sendrecGID() {
		return 0;
        }

	protected String[] sendrecCmdLine() {
		return null;
	}

	void sendRefresh() {
	}

	Auxv[] sendrecAuxv() {
		return null;
	}

}
