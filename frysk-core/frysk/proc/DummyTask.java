package frysk.proc;

public class DummyTask extends Task {

	public DummyTask(Proc parent){
		super(parent);
	}
	
	public String getStateString(){
		return "Attached";
	}
	
	protected Isa sendrecIsa() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void sendContinue(int sig) {
		// TODO Auto-generated method stub
		
	}

	protected void sendStepInstruction(int sig) {
		// TODO Auto-generated method stub
		
	}

	protected void sendStop() {
		// TODO Auto-generated method stub
		
	}

	protected void sendSetOptions() {
		// TODO Auto-generated method stub
		
	}

	protected void sendAttach() {
		// TODO Auto-generated method stub
		
	}

	protected void sendDetach(int sig) {
		// TODO Auto-generated method stub
		
	}

}
