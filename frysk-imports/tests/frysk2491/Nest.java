// variation on anoncall
public class Nest
{
    void callee () {
	System.out.println ("callee");
    }
    class Outer
    {
	class Inner
	{
	    void inner ()
	    {
		callee ();
	    }
	}
	void outer ()
	{
	    Inner i = new Inner ();
	    i.inner ();
	}
    }
    class ExtendOuter
	extends Outer
    {
	void extendOuter ()
	{
	    Inner i = new Inner ();
	    i.inner ();
	}
    }
    void nest ()
    {
	Outer o = new Outer ();
	o.outer ();
	ExtendOuter e = new ExtendOuter ();
	e.extendOuter ();
    }

    public static void main (String[] argv)
    {
	Nest n = new Nest ();
	n.nest ();
    }
}
