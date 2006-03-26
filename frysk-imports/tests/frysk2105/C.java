// From Tom Tromey
package frysk2105;
public class C {
    public static void main(String[] args) throws Throwable {
	Package p = Class.forName ("frysk2105.C").getPackage();
	System.out.println(p);
	if (p == null) throw new RuntimeException ("no package name");
    }
}
