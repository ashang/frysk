/** Stolen from Mark Wielard.  */

import java.util.logging.*;

public class LoggerParent
{
  public static void main(String[] args)
  {
    Logger ab = Logger.getLogger("a.b");
    Logger a = Logger.getLogger("a");
    Logger ac = Logger.getLogger("a.c");
    System.out.println("a: " + a);
    System.out.println("ab parent: " + ab.getParent());
    System.out.println("ac parent: " + ac.getParent());
    if (ab.getParent () != a)
	throw new RuntimeException ("ab's parent is not a");
    if (ac.getParent () != a)
	throw new RuntimeException ("ac's parent is not a");
  }
}
