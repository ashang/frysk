package frysk.proc;

public class DummyTask extends Task {

    public DummyTask (Proc parent)
    {
	super (parent, (TaskObserver.Attached) null);
    }
	
	public String getStateString(){
		return "Attached";
	}
	
	protected Isa sendrecIsa() {
		return null;
	}

	protected void sendContinue(int sig) {
	}

	protected void sendStepInstruction(int sig) {
	}

	protected void sendStop() {
	}

	protected void sendSetOptions() {
	}

	protected void sendAttach() {
	}

	protected void sendDetach(int sig) {
	}

	protected void sendSyscallContinue(int sig) {
	}

	protected void startTracingSyscalls() {
	}

	protected void stopTracingSyscalls() {
	}

  protected void fillMemory ()
  {
  }

  protected void fillRegisterBank ()
  {
  }

}
