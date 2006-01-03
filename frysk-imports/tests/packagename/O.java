// From Tom Tromey
package packagename;
public class O {
  public static void main(String[] args) throws Throwable {
    Package p = O.class.getPackage();
    System.out.println(p);
    if (p == null) throw new RuntimeException ("no package name");
  }
}
