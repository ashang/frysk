import frysk.sys.Pty;

public class PtyTest
{
    static {
	System.loadLibrary ("frysk-junit");
	System.loadLibrary ("frysk-sys");
    }

    PtyTest() {
        Pty pty = new Pty();
        int master = pty.getFd();
        String name = pty.getName();
        System.out.println ("master = " + master);
        System.out.println ("name   = " + name);
    }

    public static void main(String[] args) {
        new PtyTest();
    }
}
