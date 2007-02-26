// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.junit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for comparing kernel versions strings.
 */
public class KernelVersion
{
  private static Pattern kernelPattern;
  private static Pattern fedoraPattern;
  private static Pattern vanillaPattern;
  private int version = 0;
  private int patchLevel = 0;
  private int subLevel = 0;
  private int extraLevel = 0;
  private String extraVersion = null;
  private boolean isFedora = false;
  private boolean isVanilla = false;
  private int fedoraRelease = 0;
  private int fedoraMajor = 0;
  private int fedoraMinor = 0;

  public int getVersion()
  {
    return version;
  }

  public int getPatchLevel()
  {
    return patchLevel;
  }

  public int getSubLevel()
  {
    return subLevel;
  }

  public boolean isVanilla()
  {
    return isVanilla;
  }
  
  public String getExtraVersion()
  {
    return extraVersion;
  }

  public boolean isFedora()
  {
    return isFedora;
  }
  
  public int getFedoraRelease()
  {
    return fedoraRelease;
  }

  public int getFedoraMajor()
  {
    return fedoraMajor;
  }

    public int getFedoraMinor()
  {
    return fedoraMinor;
  }

  /**
   * Construct a kernel version object using the kernel release string
   * from uname.
   * @param release the release string, as returned by "uname -r".
   */
  public KernelVersion(String release)
  {
    if (kernelPattern == null)
      {
	kernelPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
	fedoraPattern = Pattern.compile("^-(\\d+).(\\d+)\\.fc(\\d+)(.*)$");
	vanillaPattern = Pattern.compile("^\\.(\\d+)$");
      }
    Matcher kernelMatcher = kernelPattern.matcher(release);
    if (!kernelMatcher.lookingAt())
      throw new IllegalArgumentException(release
					 + " is not a recognized kernel version number");
    version = Integer.parseInt(kernelMatcher.group(1));
    patchLevel = Integer.parseInt(kernelMatcher.group(2));
    subLevel = Integer.parseInt(kernelMatcher.group(3));
    int extra = kernelMatcher.end();
    extraVersion = release.substring(extra);
    Matcher fedoraMatcher = fedoraPattern.matcher(extraVersion);
    if (fedoraMatcher.lookingAt())
      {
	isFedora = true;
	fedoraMajor = Integer.parseInt(fedoraMatcher.group(1));
	fedoraMinor = Integer.parseInt(fedoraMatcher.group(2));
	fedoraRelease = Integer.parseInt(fedoraMatcher.group(3));
	return;
      }
    Matcher vanillaMatcher = vanillaPattern.matcher(extraVersion);
    if (vanillaMatcher.lookingAt())
      {
	isVanilla = true;
	extraLevel = Integer.parseInt(vanillaMatcher.group(1));
      }
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof KernelVersion))
      return false;
    KernelVersion kv = (KernelVersion)o;
    if (version != kv.version
	|| patchLevel != kv.patchLevel
	|| subLevel != kv.subLevel)
      return false;
    if (isFedora && kv.isFedora)
      {
	if (fedoraRelease == kv.fedoraRelease && fedoraMajor == kv.fedoraMajor
	    && fedoraMinor == kv.fedoraMinor)
	  return true;
	else
	  return false;
      }
    else if (isVanilla && kv.isVanilla)
      return extraLevel == kv.extraLevel;
    else if (extraVersion.equals(kv.extraVersion))
      return true;
    else
      return false;
  }
  
  /**
   * Tests if this kernel version is more recent than kv.
   * @param kv the KernelVersion to test against.
   * @return true if this KernelVersion is newer than kv
   */
  public boolean newer(KernelVersion kv)
  {
    if (version > kv.version)
      return true;
    else if (version < kv.version)
      return false;
    if (patchLevel > kv.patchLevel)
      return true;
    else if (patchLevel < kv.patchLevel)
      return false;
    if (subLevel > kv.subLevel)
      return true;
    else if (subLevel < kv.subLevel)
      return false;
    if (isFedora && kv.isFedora)
      {
	if (fedoraRelease > kv.fedoraRelease)
	  return true;
	else if (fedoraRelease < kv.fedoraRelease)
	  return false;
	if (fedoraMajor > kv.fedoraMajor)
	  return true;
	else if (fedoraMajor < kv.fedoraMajor)
	  return false;
	return fedoraMinor > kv.fedoraMinor;
      }
    else if (isVanilla && kv.isVanilla)
      {
	return extraLevel > kv.extraLevel;
      }
    else
      {
	// Can't tell
	return false;
      }
  }
}
