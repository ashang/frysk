package frysk;

public class CompilerVersion {

    public static native int getVersion();
    public static native int getMinorVersion();
    public static native int getPatchLevel();
    public static native int getRHRelease();
}
