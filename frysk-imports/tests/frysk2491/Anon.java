public abstract class Anon
{
    static void staticCallee () {
	System.out.println ("staticCallee");
    }
    void callee () {
	System.out.println ("callee");
    }
    abstract void caller ();
    public static void main (String[] argv)
    {
	Anon a = new Anon () {
		void caller () {
		    staticCallee ();
		    callee ();
		}
	    };
	a.caller ();
    }
}
