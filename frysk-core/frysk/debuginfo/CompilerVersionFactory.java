package frysk.debuginfo;

import java.util.HashMap;
import frysk.rsl.Log;

public class CompilerVersionFactory {
    private static HashMap compilerVersions = new HashMap();
    protected static Log fine = Log.fine(CompilerVersionFactory.class);
    protected static Log finest = Log.finest(CompilerVersionFactory.class);

    public static CompilerVersion getCompilerVersion(String compiler) {
	if (compilerVersions.containsKey(compiler))
	    return (CompilerVersion) compilerVersions.get(compiler);

	// XXX: GNU C specific.
	fine.log("Found compiler: ", compiler);

	CompilerVersion compVersion;

	// String looks like: GNU C++ 4.1.2 20070925 (Red Hat 4.1.2-33)
	//
	if (!compiler.matches("GNU C.*\\(Red Hat \\d+\\.\\d+\\.\\d+-\\d+\\)")) {
	    compVersion = new CompilerVersion(compiler);
	} else {
	    String preCompilerVersion = "(Red Hat ";

	    String compilerVersion = compiler.substring(compiler
		    .indexOf(preCompilerVersion)
		    + preCompilerVersion.length(), compiler.lastIndexOf(')'));

	    String[] versions = compilerVersion.split("\\.");

	    finest.log("Version string has 3 sections");

	    int version = Integer.parseInt(versions[0]);
	    int minorVersion = Integer.parseInt(versions[1]);

	    String[] minorVersions = versions[2].split("-");

	    int patchLevel = Integer.parseInt(minorVersions[0]);
	    int RHRelease = Integer.parseInt(minorVersions[1]);
	    compVersion = new GNURedHatCompilerVersion(compiler, version,
		    minorVersion, patchLevel, RHRelease);
	}
	compilerVersions.put(compiler, compVersion);
	return compVersion;
    }
}
