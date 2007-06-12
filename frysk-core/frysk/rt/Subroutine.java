package frysk.rt;

import lib.dw.DwarfDie;
import frysk.debuginfo.DebugInfo;

/**
 * In DWARF a subroutine is used to refer to an entity that can either
 * be a concrete function (Subprogram) or an inlined function 
 * (InlinedSubprogram).
 */
public class Subroutine extends Scope
{

  public Subroutine (DwarfDie die, DebugInfo debugInfo)
  {
    super(die, debugInfo);
  }

  public Subroutine ()
  {
  }

  
}
