
  // This file is part of FRYSK.
  //
  // Copyright 2005, Red Hat Inc.
  //
  // FRYSK is free software; you can redistribute it and/or modify
  // it under the terms of the GNU General Public License as published by
  // the Free Software Foundation; either version 2 of the License, or
  // (at your option) any later version.
  //
  // FRYSK is distributed in the hope that it will be useful,
  // but WITHOUT ANY WARRANTY; without even the implied warranty of
  // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  // GNU General Public License for more details.
  //
  // You should have received a copy of the GNU General Public License
  // along with FRYSK; if not, write to the Free Software
  // Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

  /** 
   * An that should be thrown when a <TAB> key is pressed.
   * The constructor takes an AST (abstract syntax tree) as argument. This is the
   * the partial AST generated thus far by the parser.
   */

  package frysk.expr;

  import antlr.collections.*;

  class TabException extends Exception
  {
    private AST astExpression;
    private String sTabExpression;

   /**
    * The constructor takes an AST (abstract syntax tree) as argument. This is the
    * the partial AST generated thus far by the parser. The second argument is the
    * incomplete identifier that the user may have typed in. It may or may not be null
    */
    public TabException(AST astPartial, String _sTabExpression)
    {
      astExpression = astPartial;
      sTabExpression = _sTabExpression;
    }

    /**
     * As the name suggests, this function returns the partial AST associated with this
     * Exception
     */
    public AST getAst()
    { return astExpression; }

    /**
     * Return the partial identifier that the user may have keyed in
     */
    public String getTabExpression()
    { return sTabExpression;}


    /**
     * Returns a lisp style representation of the AST
     */
    public String toString()
    {
      return astExpression.toStringList();
    }
  }
