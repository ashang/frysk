// From Tom Tromey
package packagename;
public class C {
    public static void main(String[] args) throws Throwable {
	Package p = Class.forName ("packagename.C").getPackage();
	System.out.println(p);
	if (p == null) throw new RuntimeException ("no package name");
    }
}
