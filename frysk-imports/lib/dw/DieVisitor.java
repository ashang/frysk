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

package lib.dw;

import lib.dw.die.*;

public class DieVisitor
{
  private boolean defaultThrowsException;
  
  public static class NoVisitorMethodException
    extends RuntimeException
  {
    static public final long serialVersionUID = 200703271259l;

    public DieVisitor visitor;
    public DwarfDie die;
    
    public NoVisitorMethodException(String message, DieVisitor visitor,
				    DwarfDie die)
    {
      super(message);
      this.visitor = visitor;
      this.die = die;
    }
  }

  private static final String exceptionMessage
  = "Visitor called on object with new visit method defined.";
  
  public DieVisitor(boolean defaultThrowsException)
  {
    this.defaultThrowsException = defaultThrowsException;
  }

  public DieVisitor()
  {
    this.defaultThrowsException = false;
  }

  public void visit(ArrayType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ClassType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }
  
  public void visit(EntryPoint die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(EnumerationType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(FormalParameter die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ImportedDeclaration die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Label die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(LexicalBlock die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Member die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(PointerType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ReferenceType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(CompileUnit die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(StringType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(StructureType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(SubroutineType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Typedef die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(UnionType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(UnspecifiedParameters die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Variant die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(CommonBlock die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(CommonInclusion die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Inheritance die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(InlinedSubroutine die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Module die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(PtrToMemberType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(SetType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(SubrangeType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(WithStmt die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(AccessDeclaration die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(BaseType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(CatchBlock die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ConstType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Constant die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Enumerator die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(FileType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Friend die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Namelist die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(NamelistItem die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(PackedType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Subprogram die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(TemplateTypeParameter die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(TemplateValueParameter die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ThrownType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(TryBlock die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(VariantPart die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Variable die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(VolatileType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(DwarfProcedure die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(RestrictType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(InterfaceType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Namespace die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ImportedModule die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(UnspecifiedType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(PartialUnit die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ImportedUnit die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(MutableType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(Condition die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(SharedType die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(LoUser die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(MIPSLoop die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(FormatLabel die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(FunctionTemplate die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(ClassTemplate die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

  public void visit(HiUser die)
  {
    if (defaultThrowsException)
      throw new NoVisitorMethodException(exceptionMessage, this, die);
  }

}