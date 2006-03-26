package frysk2270;

public class WrongScope
{
    public int v1 ()
    {
	class SingleName
	{
	    int v1 = 1;
	}
	// Should not compile.
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
}
