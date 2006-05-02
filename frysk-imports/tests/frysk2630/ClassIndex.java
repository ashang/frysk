public class ClassIndex
    implements Comparable
{
    static final ClassIndex x = new ClassIndex ();
    public int compareTo (Object o)
    {
	return 0;
    }
    public static void main (String[] args)
    {
	System.out.println (ClassIndex.x);
	byte[] b = new byte[ClassIndex.x];
	System.out.println ("Array length: " + b.length);
    }
}
