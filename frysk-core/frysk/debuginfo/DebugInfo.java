// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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
package frysk.debuginfo;

import antlr.CommonAST;
import frysk.dwfl.DwflCache;
import frysk.expr.CExprLexer;
import frysk.expr.CExprParser;
import frysk.expr.ExprAST;
import frysk.expr.ExprSymTab;
import frysk.expr.CExprEvaluator;
import frysk.proc.Proc;
import frysk.value.Type;
import frysk.value.Value;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import lib.dwfl.Dwarf;
import lib.dwfl.DwarfCommand;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfException;
import lib.dwfl.Dwfl;
import lib.dwfl.DwTag;
import lib.dwfl.DwAt;
import lib.dwfl.DwflDieBias;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import frysk.expr.ScratchSymTab;

public class DebugInfo {
    private Elf elf;
    private Dwarf dwarf;

    /**
     * Create a symbol table object.  There should be one SymTab per process.
     * @param frame
     */
    public DebugInfo (DebugInfoFrame frame) {
	Proc proc = frame.getTask().getProc();
	try {
	    elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
	    dwarf = new Dwarf(elf, DwarfCommand.READ, null);
	}
	catch (lib.dwfl.ElfException ignore) {
	    // FIXME: Why is this ignored?
	}
    }


    /**
     * Handle ConsoleReader Completor
     * 
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (DebugInfoFrame frame, String buffer, int cursor, List candidates) {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDieBias bias = dwfl.getDie(pc);
	DwarfDie die = bias.die;
	String token = "";

	String sInput = buffer.substring(0, cursor) + '\t' + (cursor < buffer.length() 
							      ? buffer.substring(cursor) : "");

	sInput += (char)3;
	CExprLexer lexer = new CExprLexer(new StringReader(sInput));
	CExprParser parser = new CExprParser(lexer);
	parser.setASTNodeClass("frysk.expr.ExprAST");
	try {
	    parser.start();
	} catch (antlr.RecognitionException ignore) {
	    // FIXME: Why is this ignored?
	} catch (antlr.TokenStreamException ignore) {
	    // FIXME: Why is this ignored?
	} catch (frysk.expr.TabException t) {
	    token = t.getTabExpression().trim();
	}

	DwarfDie[] allDies = die.getScopes(pc - bias.bias);
	List candidates_p = die.getScopeVarNames(allDies, token);
	boolean haveStruct = false;
	if (token.endsWith("."))
	    haveStruct = true;
	
	for (Iterator i = candidates_p.iterator(); i.hasNext();) {
            String sNext = (haveStruct ? "." : "") + (String) i.next();
            candidates.add(sNext);
        }

	if (haveStruct)
	    token = ".";
	return buffer.indexOf(token) + 1;
    }

    /**
     * Get the DwarfDie for a function symbol
     */
    public DwarfDie getSymbolDie(String input) {
	DwarfDie result = DwarfDie.getDecl(dwarf, input);
	if (result == null)
	    throw new RuntimeException("symbol " + input + " not found.");
	else
	    return result;
    }
 
    /**
     * Implement the cli what request
     * 
     * @param sInput
     * @return String
     * @throws ParseException
     */
    public String what(DebugInfoFrame frame, String sInput) throws ParseException {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDieBias bias = dwfl.getDie(pc);
	TypeEntry typeEntry = new TypeEntry(frame.getTask().getISA());
	if (bias == null)
	    throw new RuntimeException("No symbol table is available.");
	DwarfDie die = bias.die;
	StringBuffer result = new StringBuffer();

	DwarfDie[] allDies = die.getScopes(pc - bias.bias);
	DwarfDie varDie = die.getScopeVar(allDies, sInput);
	if (varDie == null) {
	    varDie = DwarfDie.getDecl(dwarf, sInput);
	    if (varDie == null)
		throw new RuntimeException(sInput + " not found in scope.");
	    if (varDie.getAttrBoolean(DwAt.EXTERNAL))
		result.append("extern ");
	    switch (varDie.getTag().hashCode()) {
            case DwTag.SUBPROGRAM_: {
		Value value = typeEntry.getSubprogramValue(varDie);
		result.append(value.getType().toPrint());
		break;
            }
            case DwTag.TYPEDEF_:
            case DwTag.STRUCTURE_TYPE_: {
		Type type = typeEntry.getType(varDie.getType());
		result.append(type.toPrint());
		break;
            }
            default:
		result.append(varDie + " " + varDie.getName());
	    }
        } else {
	    Type type = typeEntry.getType(varDie.getType());
	    if (varDie.getAttrBoolean(DwAt.EXTERNAL))
		result.append("extern ");

	    if (type != null)
		result.append(type.toPrint());
        }
	if (varDie != null) {
	    try {
		result.append(" at " + varDie.getDeclFile()
			      + "#" + varDie.getDeclLine());
	    } catch (DwarfException de) {
		result.append(" at <unknown>");
	    }
        }
	return result.toString();
    }
    
    /**
     * Implement the cli print request.
     * 
     * @param sInput
     * @return Variable
     * @throws ParseException
     */
      public Value print (String sInput, DebugInfoFrame frame) throws ParseException {
	  return print (sInput, frame, false);
      }
    
      public Value print (String sInput, DebugInfoFrame frame, boolean dumpTree) throws ParseException {
	Value result = null;
	sInput += (char) 3;
    
	CExprLexer lexer = new CExprLexer(new StringReader(sInput));
	CExprParser parser = new CExprParser(lexer);
	parser.setASTNodeClass("frysk.expr.ExprAST");
	try {
	    parser.start();
	} catch (antlr.RecognitionException r) {
	    throw new RuntimeException(r);
	} catch (antlr.TokenStreamException t) {
	    throw new RuntimeException(t);
	} catch (frysk.expr.TabException t) {
	    throw new RuntimeException(t);
	}
    
	ExprAST exprAST = (ExprAST) parser.getAST();
	if (dumpTree)
	    System.out.println("parse tree: " + exprAST.toStringTree());
	CExprEvaluator cExprEvaluator;
	/*
	 * If this request has come from the SourceWindow, there's no way to
	 * know which thread the mouse request came from; if there are multiple
	 * innermost frames of multiple threads in the same source file, than
	 * all of the threads have to be checked. If there's only one thread;
	 * than this loop will run only once anyways.
	 */
	DebugInfoEvaluator debugInfoEvaluator = new DebugInfoEvaluator(frame);
	cExprEvaluator = new CExprEvaluator(debugInfoEvaluator);
	try {
	    result = cExprEvaluator.expr(exprAST);
	} catch (ArithmeticException ae) {
	    ae.printStackTrace();
	    throw ae;
	} catch (antlr.RecognitionException r) {
	    throw new RuntimeException(r);
	} catch (frysk.value.InvalidOperatorException i) {
	    throw new RuntimeException(i);
	} catch (frysk.value.OperationNotDefinedException o) {
	    throw new RuntimeException(o);
	}
        
	return result;
    }
   
    static public Value printNoSymbolTable (String sInput, boolean dump_tree) throws ParseException {
	Value result = null;
	sInput += (char) 3;
    
	CExprLexer lexer = new CExprLexer(new StringReader(sInput));
	CExprParser parser = new CExprParser(lexer);
	parser.setASTNodeClass("frysk.expr.ExprAST");
	try {
	    parser.start();
	} catch (antlr.RecognitionException r) {
	    throw new RuntimeException(r);
	} catch (antlr.TokenStreamException t) {
	    throw new RuntimeException(t);
	}
    
	CommonAST t = (CommonAST) parser.getAST();
	if (dump_tree)
	    // Print the resulting tree out in LISP notation
	    System.out.println("parse tree: " + t.toStringTree());
	CExprEvaluator cExprEvaluator;
	ExprSymTab tmpSymTab = new ScratchSymTab();
	cExprEvaluator = new CExprEvaluator(tmpSymTab);
        
	try {
	    result = cExprEvaluator.expr(t);
	} catch (ArithmeticException ae) {
	    ae.printStackTrace();
	    throw ae;
	} catch (antlr.RecognitionException r) {
	    throw new RuntimeException(r);
	} catch (frysk.value.InvalidOperatorException i) {
	    throw new RuntimeException(i);
	} catch (frysk.value.OperationNotDefinedException o) {
	    throw new RuntimeException(o);
	}
      
	return result;
    }
}
