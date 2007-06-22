package frysk2137;

public class Gcov
{
    public static void main (String[] argv)
    {
	// gcov gets grumpy when there's no code executed so offer at
	// least one line.
	System.out.println ("Hello World");
    }
}
