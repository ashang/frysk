package samename;

public class WrongVariable
{
    public int v1 ()
    {
	class SingleName
	{
	    int v1 = 1;
	    int v2 = 1;
	}
	return new SingleName ().v2;
    }
    public int v2 ()
    {
	class SingleName
	{
	    int v2 = 2;
	}
	return new SingleName ().v2;
    }
    static public void main (String[] argv)
    {
	WrongVariable w = new WrongVariable ();
	int v1 = w.v1 ();
	if (v1 != 1)
	    throw new RuntimeException ("v1 != 1 (" + v1 + ")");
	int v2 = w.v2 ();
	if (v2 != 2)
	    throw new RuntimeException ("v2 != 2 (" + v2 + ")");
    }
}
