/** Copied from http://gcc.gnu.org/bugzilla/show_bug.cgi?id=21775 */

import java.util.logging.Level;
import java.util.logging.Logger;

public class Npe {

private static Logger logger = Logger.getLogger("foo");

  public static void main (String args []) {
	  logger.log (Level.SEVERE, "Hi", new Throwable ());
  }

}
