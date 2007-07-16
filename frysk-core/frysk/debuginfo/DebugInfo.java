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
import frysk.expr.CppLexer;
import frysk.expr.CppParser;
import frysk.expr.CppSymTab;
import frysk.expr.CppTreeParser;
import frysk.proc.Proc;
import frysk.rt.Subprogram;
import frysk.stack.Frame;
import frysk.value.FunctionType;
import frysk.value.Value;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.naming.NameNotFoundException;
import lib.dwfl.DwAtEncodings;
import lib.dwfl.DwTagEncodings;
import lib.dwfl.Dwarf;
import lib.dwfl.DwarfCommand;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfException;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;

public class DebugInfo {
    Elf elf;
    Dwarf dwarf;
  
    DebugInfoEvaluator[] debugInfoEvaluator;
  
    Subprogram[] subprogram;

    /**
     * Create a symbol table object.  There should be one SymTab per process.
     * @param frame
     */
    public DebugInfo (Frame frame) {
	Proc proc = frame.getTask().getProc();
	try {
	    elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
	    dwarf = new Dwarf(elf, DwarfCommand.READ, null);
	}
	catch (lib.dwfl.ElfException ignore) {
	    // FIXME: Why is this ignored?
	}
	debugInfoEvaluator = new DebugInfoEvaluator[1];
	subprogram = new Subprogram[1];
	debugInfoEvaluator[0] = new DebugInfoEvaluator (frame);
    }


    /**
     * Handle ConsoleReader Completor
     * 
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (Frame frame, String buffer, int cursor, List candidates) {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDieBias bias = dwfl.getDie(pc);
	DwarfDie die = bias.die;
	String token = "";

	String sInput = buffer.substring(0, cursor) + '\t' + (cursor < buffer.length() 
							      ? buffer.substring(cursor) : "");

	sInput += (char)3;
	CppLexer lexer = new CppLexer(new StringReader(sInput));
	CppParser parser = new CppParser(lexer);
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
    public DwarfDie getSymbolDie(String input)
	throws NameNotFoundException {
	DwarfDie result = DwarfDie.getDecl(dwarf, input);
	if (result == null)
	    throw new NameNotFoundException("symbol " + input + " not found.");
	else
	    return result;
    }
 
    /**
     * Implement the cli what request
     * 
     * @param sInput
     * @return String
     * @throws ParseException
     * @throws NameNotFoundException
     */
    public String what(Frame frame, String sInput) throws ParseException,
					     NameNotFoundException {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDieBias bias = dwfl.getDie(pc);
	if (bias == null)
	    throw new NameNotFoundException("No symbol table is available.");
	DwarfDie die = bias.die;
	StringBuffer result = new StringBuffer();

	DwarfDie[] allDies = die.getScopes(pc - bias.bias);
	DwarfDie varDie = die.getScopeVar(allDies, sInput);
	if (varDie == null) {
	    varDie = DwarfDie.getDecl(dwarf, sInput);
	    if (varDie == null)
		throw new NameNotFoundException(sInput + " not found in scope.");
	    if (varDie.getAttrBoolean(DwAtEncodings.DW_AT_external_))
		result.append("extern ");
	    switch (varDie.getTag()) {
            case DwTagEncodings.DW_TAG_subprogram_: {
		Value value = debugInfoEvaluator[0].getSubprogramValue(varDie);
		result.append(((FunctionType)value.getType()).getName());
		break;
            }
            case DwTagEncodings.DW_TAG_typedef_:
            case DwTagEncodings.DW_TAG_structure_type_: {
		Value value = debugInfoEvaluator[0].getValue(varDie);
		result.append(value.getType().getName());
		break;
            }
            default:
		result.append(varDie + " " + varDie.getName());
	    }
        } else {
	    Value value = debugInfoEvaluator[0].getValue(varDie);
	    if (varDie.getAttrBoolean(DwAtEncodings.DW_AT_external_))
		result.append("extern ");

	    if (value != null)
		result.append(value.getType().getName());
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
      public Value print (String sInput, Frame frame) throws ParseException,
					      NameNotFoundException {
	Value result = null;
	sInput += (char) 3;
    
	CppLexer lexer = new CppLexer(new StringReader(sInput));
	CppParser parser = new CppParser(lexer);
	try {
	    parser.start();
	} catch (antlr.RecognitionException r) {
	    // FIXME: Why is this ignored?
	} catch (antlr.TokenStreamException t) {
	    // FIXME: Why is this ignored?
	} catch (frysk.expr.TabException t) {
	    // FIXME: Why is this ignored?
	}
    
	CommonAST t = (CommonAST) parser.getAST();
	CppTreeParser treeParser;
	/*
	 * If this request has come from the SourceWindow, there's no way to
	 * know which thread the mouse request came from; if there are multiple
	 * innermost frames of multiple threads in the same source file, than
	 * all of the threads have to be checked. If there's only one thread;
	 * than this loop will run only once anyways.
	 */
	int j = 0;
	while (result == null && j < debugInfoEvaluator.length) {
	  treeParser = new CppTreeParser(4, frame, debugInfoEvaluator[j]);

	    try {
		result = treeParser.expr(t);
	    } catch (ArithmeticException ae) {
		ae.printStackTrace();
		throw ae;
	    } catch (antlr.RecognitionException r) {
		// FIXME: Why is this ignored?
	    } catch (frysk.value.InvalidOperatorException i) {
		// FIXME: Why is this ignored?
	    } catch (frysk.value.OperationNotDefinedException o) {
		// FIXME: Why is this ignored?
	    }
            
	    ++j;
	}
        
	return result;
    }
   
    static public Value printNoSymbolTable (String sInput) throws ParseException,
								  NameNotFoundException {
	Value result = null;
	sInput += (char) 3;
    
	final class TmpSymTab
	    implements CppSymTab {
	  public void put (Frame f, String s, Value v) throws NameNotFoundException {
		throw new NameNotFoundException("No symbol table is available.");
	    }

	  public Value get (Frame f, String s) throws NameNotFoundException {
		throw new NameNotFoundException("No symbol table is available.");
	    }

	  public Value get (Frame f, ArrayList v) throws NameNotFoundException {
		throw new NameNotFoundException("No symbol table is available.");
	    }
      
	public Value getAddress (Frame f, String s) throws NameNotFoundException {
		throw new NameNotFoundException("No symbol table is available.");
	    }
	  public Value getMemory (Frame f, String s) throws NameNotFoundException {
		throw new NameNotFoundException("No symbol table is available.");        
	    }
      
	    public boolean putUndefined () {
		return false;
	    }
	}
    
	CppLexer lexer = new CppLexer(new StringReader(sInput));
	CppParser parser = new CppParser(lexer);
	try {
	    parser.start();
	} catch (antlr.RecognitionException r) {
	    // FIXME: Why is this ignored?
	} catch (antlr.TokenStreamException t) {
	    // FIXME: Why is this ignored?
	} catch (frysk.expr.TabException t) {
	    // FIXME: Why is this ignored?
	}
    
	CommonAST t = (CommonAST) parser.getAST();
	CppTreeParser treeParser;
	TmpSymTab tmpSymTab = new TmpSymTab();
	treeParser = new CppTreeParser(4, null, tmpSymTab);
        
	try {
	    result = treeParser.expr(t);
	} catch (ArithmeticException ae) {
	    ae.printStackTrace();
	    throw ae;
	} catch (antlr.RecognitionException r) {
	    // FIXME: Why is this ignored?
	} catch (frysk.value.InvalidOperatorException i) {
	    // FIXME: Why is this ignored?
	} catch (frysk.value.OperationNotDefinedException o) {
	    // FIXME: Why is this ignored?
	}
      
	return result;
    }
   
    public void setFrames (Frame newFrames[]) {
	debugInfoEvaluator = new DebugInfoEvaluator[newFrames.length];
	for (int i = 0; i < newFrames.length; i++) {
	    debugInfoEvaluator[i] = new DebugInfoEvaluator (newFrames[i]);
	}
    }
     
    public Value getValue (DwarfDie die) {
	return debugInfoEvaluator[0].getValue(die);
    } 

    public Value get(Frame f, DwarfDie die) throws NameNotFoundException
    {
      return debugInfoEvaluator[0].get(f, die);
    } 

}
