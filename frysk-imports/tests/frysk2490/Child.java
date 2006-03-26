package frysk2490;

class Child
{
    int i;
    class Nested extends Parent
    {
	boolean b;
	native void f ();
    }
    Nested nested ()
    {
      return new Nested ();
    }
    static public void main (String[] args)
    {
	Child c = new Child ();
	Nested n = c.nested ();
	n.f ();
    }
}
