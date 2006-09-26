// From Tom Tromey
package frysk2105;
public class O {
  public static void main(String[] args) throws Throwable {
    Package p = O.class.getPackage();
    System.out.println(p);
    if (p == null) throw new RuntimeException ("no package name");
  }
}
