class ResponseListener implements Runnable {
  private final int IF_RESP_NULL		= 0;      	
  private final int IF_RESP_CLONE_DATA		= 1;          
  private final int IF_RESP_SIGNAL_DATA		= 2;         
  private final int IF_RESP_EXIT_DATA		= 3;           
  private final int IF_RESP_DEATH_DATA		= 4;          
  private final int IF_RESP_SYSCALL_ENTRY_DATA	= 5;  
  private final int IF_RESP_SYSCALL_EXIT_DATA	= 6;   
  private final int IF_RESP_EXEC_DATA		= 7;           
  private final int IF_RESP_QUIESCE_DATA	= 8;        
  private final int IF_RESP_SYNC_DATA		= 9;

  private final int SYNC_NULL = 0;
  private final int SYNC_INIT = 1;
  private final int SYNC_RESP = 2;
  private final int SYNC_HALT = 3;
	
  ResponseListener() {
    System.out.println ("ResponseListener()");
  }

  public void run() {
    boolean spin = true;
    while (spin) {
      System.out.println ("Starting pread");
      int type = Utrace.read_response();
      System.out.println ("Back from pread, type = " + type);
      
      switch (type) {
      case IF_RESP_NULL:
	System.out.println ("resp null");
	break;
      case IF_RESP_CLONE_DATA:
	System.out.println ("resp clone");
	break;
      case IF_RESP_SIGNAL_DATA:
	System.out.println ("resp signal");
	break;
      case IF_RESP_EXIT_DATA:
	System.out.println ("resp exit");
	break;
      case IF_RESP_DEATH_DATA:
	System.out.println ("resp death");
	break;
      case IF_RESP_SYSCALL_ENTRY_DATA:
	System.out.println ("resp syscall entry");
	break;
      case IF_RESP_SYSCALL_EXIT_DATA:
	System.out.println ("resp syscall exit");
	break;
      case IF_RESP_EXEC_DATA:
	System.out.println ("resp exec");
	break;
      case IF_RESP_QUIESCE_DATA:
	System.out.println ("resp quiesce");
	break;
      case IF_RESP_SYNC_DATA:
	System.out.println ("resp sync");
	long st = Utrace.read_response_sync_type();
	
	if (SYNC_HALT == st) spin = false;
	break;
      }
    }
  }
}	