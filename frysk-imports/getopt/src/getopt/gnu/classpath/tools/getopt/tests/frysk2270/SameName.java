public class SameName
{
    public Object foo ()
    {
	class SingleName
	{
	    public String toString ()
	    {
		return "foo.SingleName";
	    }
	}
	// This should instantiate SingleName above, instead (the mind
	// boggles) it was managing to instantiate the class SameName
	// in the function bar below.
	return new SingleName ();
    }
    public Object bar ()
    {
	class SingleName
	{
	    public String toString ()
	    {
		throw new RuntimeException ("bar.SingleName");
	    }
	}
	return new SingleName ();
    }
    public static void main (String[] args)
    {
	System.out.println (new SameName ().foo().toString());
    }
}
