// variation on anoncall
public class Nest
{
    void callee () {
	System.out.println ("callee");
    }

    class Middle
    {
	void middle ()
	{
	    callee ();
	}
    }

    public static void main (String[] argv)
    {
	Middle m = new Middle ();
	m.middle ();
    }
}
