package frysk.proc;

public class DummyHost extends Host {

	void sendRefresh(boolean refreshAll) {
	}

	void sendCreateAttachedProc(String stdin, String stdout,
				    String stderr, String[] args,
				    TaskObserver.Attached attached)
    {
	}

	protected Proc sendrecSelf() {
		return null;
	}

  void sendRefresh (ProcId procId, FindProc finder)
  {
  }

}
