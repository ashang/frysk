package frysk2488;

/** From GCC bug 26042, written by Ben Elliston.  */

class One
{
    long l;    // no ICE if this is int, not long
    int b;     // no ICE if this line is gone; type doesn't matter
}

class Two
{
    class Three extends One { }
    Three three () { return new Three (); }
}
