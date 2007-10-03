// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

/**
 * CDTParser uses the parser from the Eclipse CDT to generate static information
 * about the source file
 */


package frysk.dom.cparser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.IParser;
import org.eclipse.cdt.core.parser.IProblem;
import org.eclipse.cdt.core.parser.ISourceElementRequestor;
import org.eclipse.cdt.core.parser.NullLogService;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.core.parser.ParserMode;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ast.IASTASMDefinition;
import org.eclipse.cdt.core.parser.ast.IASTAbstractTypeSpecifierDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTClassReference;
import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
import org.eclipse.cdt.core.parser.ast.IASTCompilationUnit;
import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTEnumerationReference;
import org.eclipse.cdt.core.parser.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTEnumeratorReference;
import org.eclipse.cdt.core.parser.ast.IASTField;
import org.eclipse.cdt.core.parser.ast.IASTFieldReference;
import org.eclipse.cdt.core.parser.ast.IASTFunction;
import org.eclipse.cdt.core.parser.ast.IASTFunctionReference;
import org.eclipse.cdt.core.parser.ast.IASTInclusion;
import org.eclipse.cdt.core.parser.ast.IASTLinkageSpecification;
import org.eclipse.cdt.core.parser.ast.IASTMacro;
import org.eclipse.cdt.core.parser.ast.IASTMethod;
import org.eclipse.cdt.core.parser.ast.IASTMethodReference;
import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
import org.eclipse.cdt.core.parser.ast.IASTNamespaceReference;
import org.eclipse.cdt.core.parser.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTParameterReference;
import org.eclipse.cdt.core.parser.ast.IASTTemplateDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTTemplateInstantiation;
import org.eclipse.cdt.core.parser.ast.IASTTemplateParameterReference;
import org.eclipse.cdt.core.parser.ast.IASTTemplateSpecialization;
import org.eclipse.cdt.core.parser.ast.IASTTypedefDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTTypedefReference;
import org.eclipse.cdt.core.parser.ast.IASTUsingDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTUsingDirective;
import org.eclipse.cdt.core.parser.ast.IASTVariable;
import org.eclipse.cdt.core.parser.ast.IASTVariableReference;

import frysk.dom.DOMImage;
import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.dom.DOMFunction;
import frysk.dom.DOMTagTypes;
import frysk.dom.StaticParser;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMCompilerSuffixes;

public class CDTParser
    implements StaticParser
{

  private DOMSource source;
  
  private final boolean debug = false;
  
  private final String DEFINE = "#define";
  
  private boolean quickparse = true;
  
  private int number_includes = 0;
  
  // HashSet for keeping up with includes
  private HashSet ic = new HashSet();
  
  // HashSet for keeping up with Macro defines(#DEFINE)
  private HashSet md = new HashSet();
  
  /*
   * (non-Javadoc)
   * 
   * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String,
   *      frysk.gui.srcwin.SourceBuffer)
   */
  public void parse (DOMFrysk dom, DOMSource source, DOMImage image)
    throws IOException
  {
    this.source = source;
    File filename = new File(source.getFilePath(), source.getFileName());
    ParserLanguage language;
    // See what kind of file we are handed so we can tell the parser how to scan
    if (DOMCompilerSuffixes.checkCPP(filename.toString()))
      {
        language = ParserLanguage.CPP;
      }
    else if (DOMCompilerSuffixes.checkC(filename.toString()))
      {
        language = ParserLanguage.C;
      }
    // If this is neither a c or a c++ type file, just return
    else
      return;
    
    String [] incPaths = getIncPaths(this.source);
    IScannerInfo buildScanInfo = new ScannerInfo(null, incPaths);
    IScannerInfo scanInfo = new ScannerInfo(buildScanInfo.getDefinedSymbols(),
                                            buildScanInfo.getIncludePaths());

    ParserCallBack callback = new ParserCallBack();
    if (debug)
      {
        System.out.println("\n\n***** Fixing to parse " + filename.toString() + " *****\n\n");
      }
    IParser parser = ParserFactory.createParser(
                                                ParserFactory.createScanner(
                                                                            filename.toString(),
                                                                            new ScannerInfo(),
                                                                            ParserMode.QUICK_PARSE,
                                                                            language,
                                                                            callback,
                                                                            new NullLogService(),
                                                                            null),
                                                callback,
                                                ParserMode.QUICK_PARSE,
                                                language, new NullLogService());

    if (! parser.parse() && debug)
      System.err.println("Quick Parse: Error found on line "
                         + parser.getLastErrorLine());
    
    if (debug)
      System.out.println("\n\n*********** After quick parse for " + filename.toString() + 
                         " ***********\n\n");

    quickparse = false;
    ParserCallBack callback2 = new ParserCallBack();
    IParser parser2 = ParserFactory.createParser(
                                                 ParserFactory.createScanner(
                                                                             filename.toString(),
                                                                             scanInfo,
                                                                             ParserMode.COMPLETE_PARSE,
                                                                             language,
                                                                             callback2,
                                                                             new NullLogService(),
                                                                             null),
                                                 callback2,
                                                 ParserMode.COMPLETE_PARSE,
                                                 language, new NullLogService());

    if (! parser2.parse() && debug)
      System.err.println("Complete Parse: Error found on line "
                         + parser2.getLastErrorLine()
                         + "\n                Char offset of error = "
                         + parser2.getLastErrorOffset());

    /*
     * The CDT Parser does not parse out comments for some reason, do a second
     * parsing run and pick them out
     */
//    Tokenizer tokenMaker = new Tokenizer(filename);

//    while (tokenMaker.hasMoreTokens())
//      {
//        Token t = tokenMaker.nextToken();

        // C++ style comments
//        if (t.text.equals("//"))
//          {
//            Token t2 = t;
//            while (tokenMaker.hasMoreTokens()
//                   && tokenMaker.peek().lineNum == t.lineNum)
//              {
//                t2 = tokenMaker.nextToken();
//              }
//            t2.toString();
            // buffer.addComment(t.lineNum, t.colNum, t.lineNum,
            // t2.colNum+t2.text.length());
//          }
        // C Style comments
//        else if (t.text.equals("/*"))
//          {
//            Token t2 = t;
//            while (tokenMaker.hasMoreTokens()
//                   && ! tokenMaker.peek().text.equals("*/"))
//              {
//                t2 = tokenMaker.nextToken();
//              }
//            t2 = tokenMaker.nextToken();
//            t2.toString();
            // buffer.addComment(t.lineNum, t.colNum, t2.lineNum,
            // t2.colNum+t2.text.length());
//          }
        // For some reason the CDTParser doesn't pick up this keyword either
//        else if (t.text.equals("return") | t.text.startsWith("exit("))
//          {
//            DOMLine line = this.source.getLine(t.lineNum + 1);
//            if (line == null)
//              return;

//            line.addTag(DOMTagTypes.KEYWORD, t.text, t.colNum);
//          }
//      }
    
  }
  
  /*
   * getIncPaths gets the "include" paths from the DOMSource Element
   * 
   * @return returns an array of strings with the include paths,
   *                    null if there are none
   */
  
  public String[] getIncPaths (DOMSource source)
  {
    String includes;
    includes = source.getIncludes();
    if (includes.equals(""))
      return null;

    // Allocate the correct size string array
    String[] incpaths = new String[countCommas(includes)+1];
    int pathctr = 0;
    int ptr = 0;
    int j = 0;
    // Loop thru the provided string and pull out the comma-separated
    // include paths with each path in its own string
    while (j < includes.length()) {
      j = includes.indexOf(",", ptr);
      // If we've found the last comma, we have one more include path
      if (j == -1)
          j = includes.length();
      incpaths[pathctr] = includes.substring(ptr,j);
      ptr = includes.indexOf(",", ptr) + 1;
      pathctr++;
    }
    return incpaths;
  }
  
  /**
   * get the number of commas in the "includes" element of this source
   * 
   * @return an integet containing the number of commas
   *
   */
  
  public int countCommas(String includes) {
    int ctr = 1;
    int comma_index = 0;
    int i = 0;
    while (comma_index != -1) {
        comma_index = includes.indexOf(",", i);
        if (i != -1) {
            ctr++;
            i = comma_index + 2;
        }
    }
    return ctr;
}

  class ParserCallBack
      implements ISourceElementRequestor
  {

    public void acceptVariable (IASTVariable arg0)
    {
      if (debug)
        System.out.println("made it to acceptVariable.....name = " + arg0.getName());
      // Don't assume the type is on the same line as the name
      DOMLine typeLine = source.getLineSpanningOffset(arg0.getStartingOffset());
      DOMLine nameLine = source.getLineSpanningOffset(arg0.getNameOffset());
      if (typeLine == null || nameLine == null)
        return;

      String typeText = typeLine.getText();
      String nameText = nameLine.getText();
      if (debug)
        System.out.println(".....typeText = " + typeText.trim() +
                           ".....nameText = " + nameText.trim());
      
      // Check to see if the character string we are parsing is in one of the lines
      if (!checkScope(arg0.getName(), typeText) || !checkScope(arg0.getName(), nameText))
        return;
      
      String token = typeText.substring(arg0.getStartingOffset()
                                        - typeLine.getOffset(),
                                        arg0.getNameOffset()
                                            - typeLine.getOffset()).trim();

      typeLine.addTag(DOMTagTypes.KEYWORD,
                      token.trim(),
                      arg0.getStartingOffset() - typeLine.getOffset());
      nameLine.addTag(DOMTagTypes.LOCAL_VAR,
                      nameText.substring(arg0.getNameOffset()
                                         - nameLine.getOffset(),
                                         arg0.getNameOffset()
                                             - nameLine.getOffset()
                                             + arg0.getName().length()).trim(),
                      arg0.getNameOffset() - nameLine.getOffset());
    }

    public void acceptFunctionDeclaration (IASTFunction arg0)
    {
      if (debug)
        System.out.println("made it to acceptFunctionDeclaration.....name = " + arg0.getName());
      
      // The return type of the function may not be on the same line as the name
      DOMLine line = source.getLine(arg0.getStartingLine());
      DOMLine nameLine = source.getLineSpanningOffset(arg0.getNameOffset());
      if (line == null || nameLine == null)
        return;

      String lineText = line.getText();
      String nameText = nameLine.getText();
      if (debug)
        System.out.println(".....lineText = " + lineText.trim() +
                          ".....nameText = " + nameText.trim());
      
      // Check to see if the character string we are parsing is in one of the lines
      if (!checkScope(arg0.getName(), lineText) || !checkScope(arg0.getName(), nameText))
        return;

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()).trim(),
                  arg0.getStartingOffset() - line.getOffset());
      nameLine.addTag(DOMTagTypes.FUNCTION,
                      nameText.substring(arg0.getNameOffset()
                                         - nameLine.getOffset(),
                                         arg0.getNameOffset()
                                             - nameLine.getOffset()
                                             + arg0.getName().length()).trim(),
                      arg0.getNameOffset() - nameLine.getOffset());

      Iterator iter = arg0.getParameters();
      while (iter.hasNext())
        {
          IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();

          DOMLine typeLine = null, paramLine = null;
          String typeText = "";

          /*
           * Just like we didn't assume that the return type and name were on
           * the same line, we don't assume that the param types and names are
           * on the same line or that all the parameters are on the same line.
           * At the same time though we want to be reasonably efficient if they
           * are.
           */
          if (param.getStartingLine() != nameLine.getLineNum())
            {
              typeLine = source.getLine(param.getStartingLine());
              typeText = typeLine.getText();
            }
          else
            {
              typeLine = nameLine;
              typeText = nameText;
            }

          // There may not be a parameter name in a function delcaration
          if (param.getNameOffset() != - 1)
            {
              /*
               * Perform compairasons relative to the parameter type line, so
               * that if the parameters are both on the same line we still get a
               * little better performance.
               */
              if (param.getNameLineNumber() != typeLine.getLineNum())
                {
                  paramLine = source.getLine(param.getNameLineNumber());
                }
              else
                {
                  paramLine = typeLine;
                }

              typeLine.addTag(DOMTagTypes.KEYWORD,
                              typeText.substring(param.getStartingOffset()
                                                 - typeLine.getOffset(),
                                                 param.getNameOffset()
                                                     - typeLine.getOffset()),
                              param.getStartingOffset() - typeLine.getOffset());
              paramLine.addTag(DOMTagTypes.LOCAL_VAR, param.getName(),
                               param.getNameOffset() - paramLine.getOffset());
            }
          // There is no parameter name, so only make a tag for the keyword
          else
            {
              /*
               * TODO: As of right now, it seems that whenever the parameter
               * name doesn't exist, param.getEndingOffset() is 0, which makes
               * it impossible to determine the end of this tag. When this is
               * fixed uncommment this line
               */
              // typeLine.addTag(DOMTagTypes.KEYWORD,
              // typeText.substring(param.getStartingOffset() -
              // typeLine.getOffset(), param.getEndingOffset() -
              // typeLine.getOffset()), param.getStartingOffset() -
              // typeLine.getOffset());
            }
        }
    }

    public void enterFunctionBody (IASTFunction arg0)
    {
      if (quickparse)
        return;
      if (debug) 
        System.out.println("enterFunctionBody.....name = " + arg0.getName());
    
      // The return type of the function may not be on the same line as the name
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      DOMLine nameLine = source.getLineSpanningOffset(arg0.getNameOffset());
      if (line == null || nameLine == null)
          return;

      String lineText = line.getText().trim();
      String nameText = nameLine.getText().trim();
      if (debug)
          System.out.println(".....lineText = " + lineText.trim() +
                             "\n.....nameText = " + nameText.trim());
      // Check to see if the character string we are parsing is in one of the lines
      if ((!checkScope(arg0.getName(), lineText) && !checkScope(arg0.getName(), nameText))
        || number_includes > 0)
        return;
      
      // On functions that are pointers(have an "*" in front of the name) the CDTParser handles
      // them weirdly so when that occurs we must adjust the length of the substring parameters
      int startingcharindex = arg0.getStartingOffset() - line.getOffset();
      int endingcharindex = arg0.getNameOffset() - line.getOffset();
      // Make sure the following substring does not go beyond the line length
      if (endingcharindex > lineText.length())
        endingcharindex = lineText.length();
      
      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(startingcharindex, endingcharindex),
                  arg0.getStartingOffset() - line.getOffset());
      nameLine.addTag(DOMTagTypes.FUNCTION, arg0.getName(), arg0.getNameOffset()
                                                      - nameLine.getOffset());
      
      // start building the full name of the function for jump-to purposes
      String functionName = arg0.getName() + "(";

      Iterator iter = arg0.getParameters();
      int length;
      while (iter.hasNext())
        {
          IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
          
          // If no parameter has been returned from the parser, skip this
          if (param.getName().equals(""))
            break;

          DOMLine typeLine = null, paramLine = null;
          String typeText = "";

          /*
           * Just like we didn't assume that the return type and name were on
           * the same line, we don't assume that the param types and names are
           * on the same line or that all the parameters are on the same line.
           * At the same time though we want to be reasonably efficient if they
           * are.
           */
          if (param.getStartingLine() != nameLine.getLineNum())
            {
              typeLine = source.getLineSpanningOffset(param.getStartingOffset());
              typeText = typeLine.getText();
            }
          else
            {
              typeLine = nameLine;
              typeText = nameText;
            }

          /*
           * Perform comparisons relative to the parameter type line, so that if
           * the parameters are both on the same line we still get a little
           * better performance.
           */
          if (param.getEndingLine() != typeLine.getLineNum())
            {
              paramLine = source.getLineSpanningOffset(param.getEndingOffset());
            }
          else
            {
              paramLine = typeLine;
            }

          length = param.getNameOffset() - typeLine.getOffset();
          if (length > typeText.length())
            length = typeText.length();
          String type = typeText.substring(param.getStartingOffset()
                                           - typeLine.getOffset(),
                                           length);

          String name = param.getName();

          typeLine.addTag(DOMTagTypes.KEYWORD, type, param.getStartingOffset()
                                                     - typeLine.getOffset());
          paramLine.addTag(DOMTagTypes.LOCAL_VAR, name, param.getNameOffset()
                                                        - paramLine.getOffset());

          functionName += type + " " + name + ", ";
        }

      // Trim the trailing ',' off the function name if it's there
      if (functionName.indexOf(",") != - 1)
        functionName = functionName.substring(0, functionName.length() - 2);

      functionName += ")";

      line.addTag(DOMTagTypes.FUNCTION_CALL, functionName, 0);
      // Create a DOMFunction(let exitFunctionBody set the ending line and char #'s)

      source.addFunction(arg0.getName(), arg0.getStartingLine() - 1,
                        0, arg0.getStartingOffset(), 0, functionName);
    }

    public void acceptFunctionReference (IASTFunctionReference arg0)
    {
      if (debug)
        System.out.println("made it to acceptFunctionReference.....name = " + 
                           arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText())
          || number_includes > 0)
        return;
      if (debug)
        System.out.println(".....lineText = " + line.getText());
      
      if (!checkForMacro(line))
        line.addTag(DOMTagTypes.FUNCTION, arg0.getName(), arg0.getOffset()
                                                         - line.getOffset());
      else
        line.addTag(DOMTagTypes.FUNCTION, arg0.getName(), 
            checkVariableIndex(line.getText(), arg0.getName()));
    }

    public void acceptTypedefDeclaration (IASTTypedefDeclaration arg0)
    {
      if (debug)
        System.out.println("made it to acceptTypedefDeclaration.....name = " + 
                           arg0.getName());
    }

    public void acceptTypedefReference (IASTTypedefReference arg0)
    {
      if (debug)
        System.out.println("made it to acceptTypedefReference.....name = " + 
                           arg0.getName());
    }

    public void acceptEnumerationSpecifier (IASTEnumerationSpecifier arg0)
    {
      if (debug)
        System.out.println("made it to acceptEnumerationSpecifier.....name = " + 
                           arg0.getName());
    }

    public void enterNamespaceDefinition (IASTNamespaceDefinition arg0)
    {
      if (debug)
        System.out.println("made it to enterNamespaceDefinition.....name = " + 
                           arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(line.getText(), arg0.getName()))
        return;

      String lineText = line.getText();
      if (debug)
        System.out.println(".....lineText = " + lineText);

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      line.addTag(DOMTagTypes.NAMESPACE,
                  lineText.substring(arg0.getNameOffset() - line.getOffset(),
                                     arg0.getNameOffset() - line.getOffset()
                                         + arg0.getName().length()),
                  arg0.getNameOffset() - line.getOffset());
    }

    public void acceptNamespaceReference (IASTNamespaceReference arg0)
    {
      if (debug)
        System.out.println("made it to accept NamespaceReference.....name = " + 
                           arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;
      if (debug)
        System.out.println(".....lineText = " + line.getText());

      line.addTag(DOMTagTypes.NAMESPACE, arg0.getName(), arg0.getOffset()
                                                         - line.getOffset());
    }

    public void acceptUsingDirective (IASTUsingDirective arg0)
    {
      if (debug)
        System.out.println("made it to acceptUsingDirective.....name = " + 
                           arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      String lineText = line.getText();
      if (debug)
        System.out.println(".....lineText = " + lineText);

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      // line.addTag(DOMTagTypes.NAMESPACE,
      // lineText.substring(arg0.getNameOffset() - line.getOffset(),
      // arg0.getNameOffset() - line.getOffset() + arg0.getName().length()),
      // arg0.getNameOffset() - line.getOffset());
    }

    public void acceptUsingDeclaration (IASTUsingDeclaration arg0)
    {
      if (debug)
        System.out.println("made it to acceptUsingDeclaration.....name = " + 
                           arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      String lineText = line.getText();
      if (debug)
        System.out.println(".....lineText = " + lineText);

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      // line.addTag(DOMTagTypes.CLASS_DECL,
      // lineText.substring(arg0.getNameOffset() - line.getOffset(),
      // arg0.getNameOffset() - line.getOffset() + arg0.getName().length()),
      // arg0.getNameOffset() - line.getOffset());
    }

    public void enterClassSpecifier (IASTClassSpecifier arg0)
    {
      if (debug)
          System.out.println("made it to enterClassSpecifier.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());

      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      String lineText = line.getText();
      if (debug)
          System.out.print(".....lineText = " + lineText.trim());

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      line.addTag(DOMTagTypes.CLASS_DECL,
                  lineText.substring(arg0.getNameOffset() - line.getOffset(),
                                     arg0.getNameOffset() - line.getOffset()
                                         + arg0.getName().length()),
                  arg0.getNameOffset() - line.getOffset());
    }

    public void acceptField (IASTField arg0)
    {
      if (debug)
          System.out.println("made it to acceptField.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if ((line == null || !checkScope(arg0.getName(), line.getText())) ||
          number_includes > 0)
        return;

      String lineText = line.getText();
      if (debug)
          System.out.println(".....lineText = " + lineText.trim());
      
      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      line.addTag(DOMTagTypes.LOCAL_VAR,
                  lineText.substring(arg0.getNameOffset() - line.getOffset(),
                                     arg0.getNameOffset() - line.getOffset()
                                         + arg0.getName().length()),
                  arg0.getNameOffset() - line.getOffset());
    }

    public void acceptClassReference (IASTClassReference arg0)
    {
      if (debug)
          System.out.println(".....made it to acceptClassReference.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());

      line.addTag(DOMTagTypes.CLASS_DECL, arg0.getName(), arg0.getOffset()
                                                          - line.getOffset());
    }

    public void acceptVariableReference (IASTVariableReference arg0)
    {
      if (debug)
          System.out.println("made it to acceptVariableReference.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || number_includes > 0)
        return;
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());
      
      if (!checkForMacro(line))
        line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), arg0.getOffset()
                                                         - line.getOffset());
      else {
          line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), 
              checkVariableIndex(line.getText(), arg0.getName()));
      }
    }

    public void acceptFieldReference (IASTFieldReference arg0)
    {
      if (debug)
          System.out.println("made it to acceptFieldReference.....name = " + 
                             arg0.getName());
    }

    public void acceptParameterReference (IASTParameterReference arg0)
    {
      if (debug)
          System.out.println("made it to acceptParameterReference.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText())
          || number_includes > 0)
        return;
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());

      if (!checkForMacro(line))
        line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), arg0.getOffset()
                                                         - line.getOffset());
      else
        line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), 
            checkVariableIndex(line.getText(), arg0.getName()));
    }

    public void acceptAbstractTypeSpecDeclaration (
                                                   IASTAbstractTypeSpecifierDeclaration arg0)
    {
      if (debug)
          System.out.println("made it to acceptAbstractTypeSpecDeclaration.....name = " +
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());

      line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(),
                  arg0.getStartingOffset() - line.getOffset());
    }

    /* METHODS */

    public void acceptMethodDeclaration (IASTMethod arg0)
    {
      if (debug)
          System.out.println("made it to acceptMethodDeclaration.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      String lineText = line.getText();
      if (debug)
        System.out.println(".....lineText = " + lineText.trim());

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      line.addTag(DOMTagTypes.FUNCTION,
                  lineText.substring(arg0.getNameOffset() - line.getOffset(),
                                     arg0.getNameOffset() - line.getOffset()
                                         + arg0.getName().length()),
                  arg0.getNameOffset() - line.getOffset());

      Iterator iter = arg0.getParameters();
      while (iter.hasNext())
        {
          IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
          int nameOffset = param.getNameOffset();
          if (nameOffset != - 1)
            {
              line.addTag(DOMTagTypes.KEYWORD,
                          lineText.substring(param.getStartingOffset()
                                             - line.getOffset(),
                                             param.getNameOffset()
                                                 - line.getOffset()),
                          param.getStartingOffset() - line.getOffset());
              line.addTag(DOMTagTypes.LOCAL_VAR,
                          lineText.substring(param.getNameOffset()
                                             - line.getOffset(),
                                             param.getNameOffset()
                                                 - line.getOffset()
                                                 + param.getName().length()),
                          param.getNameOffset() - line.getOffset());
            }
          else
            {
              // Figure out how to do function declarations of type "foo(int)"
              // here
            }
        }
    }

    public void acceptMethodReference (IASTMethodReference arg0)
    {
      if (debug)
          System.out.println("acceptMethodReference.....name = " +
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;
      
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());

      line.addTag(DOMTagTypes.FUNCTION, arg0.getName(), arg0.getOffset()
                                                        - line.getOffset());
    }

    public void enterMethodBody (IASTMethod arg0)
    {
      if (debug)
          System.out.println("made it to enterMethodBody.....name = " + 
                             arg0.getName());
      
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      String lineText = line.getText();
      if (debug)
          System.out.println(".....lineText = " + lineText.trim());

      line.addTag(DOMTagTypes.KEYWORD,
                  lineText.substring(arg0.getStartingOffset()
                                     - line.getOffset(), arg0.getNameOffset()
                                                         - line.getOffset()),
                  arg0.getStartingOffset() - line.getOffset());
      line.addTag(DOMTagTypes.FUNCTION,
                  lineText.substring(arg0.getNameOffset() - line.getOffset(),
                                     arg0.getNameOffset() - line.getOffset()
                                         + arg0.getName().length()),
                  arg0.getNameOffset() - line.getOffset());

      String functionName = arg0.getName() + "(";

      Iterator iter = arg0.getParameters();

      while (iter.hasNext())
        {
          IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();

          line.addTag(DOMTagTypes.KEYWORD,
                      lineText.substring(param.getStartingOffset()
                                         - line.getOffset(),
                                         param.getNameOffset()
                                             - line.getOffset()),
                      param.getStartingOffset() - line.getOffset());
          line.addTag(DOMTagTypes.LOCAL_VAR,
                      lineText.substring(param.getNameOffset()
                                         - line.getOffset(),
                                         param.getNameOffset()
                                             - line.getOffset()
                                             + param.getName().length()),
                      param.getNameOffset() - line.getOffset());

          functionName += lineText.substring(param.getStartingOffset()
                                             - line.getOffset(),
                                             param.getNameOffset()
                                                 - line.getOffset()
                                                 + param.getName().length())
                          + ", ";
        }

      if (functionName.indexOf(",") != - 1)
        functionName = functionName.substring(0, functionName.length() - 2);

      functionName += ")";

      line.addTag(DOMTagTypes.FUNCTION_CALL, functionName, 0);
    }

    /* TEMPLATES */
    public void enterTemplateDeclaration (IASTTemplateDeclaration arg0)
    {
      if (debug)
        System.out.println("made it to enterTemplateDeclaration.....name = " +
                           arg0.getClass().getName());
    }

    public void enterTemplateInstantiation (IASTTemplateInstantiation arg0)
    {
      if (debug)
        System.out.println("made it to enterTemplateInstantiation.....name = " +
                           arg0.getClass().getName());
    }

    public void enterTemplateSpecialization (IASTTemplateSpecialization arg0)
    {
      if (debug)
        System.out.println("made it to enterTemplateSpecialization.....name = " +
                           arg0.getClass().getName());
    }

    public void acceptTemplateParameterReference (
                                                  IASTTemplateParameterReference arg0)
    {
      if (debug)
        System.out.println("acceptTemplateParameterReference.....name = " +
                           arg0.getName());
   
      DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;

      line.addTag(DOMTagTypes.TEMPLATE, arg0.getName(), arg0.getOffset()
                                                        - line.getOffset());
    }

    /* PREPROCESSOR STUFF */
    
    /*
     * enterInclusion/exitInclusion use a HashSet(hs) to help determine when
     * the parser is inside of an "#include" when it returns a function/variable/
     * keyword.  If the parser is inside an "#include", the variable
     * "number_includes" is > 0.  When this is the case, the callback can be
     * ignored.
     * 
     * **NOTE** During testing is was found that if an "#include" is recursive
     * (that is an "#include" includes an "#include" which in turn includes
     * the originator, there can be multiple "enterInclusion" callbacks
     * but there will be only one "exitInclusion" callback for the originator,
     * hence the reason for the HashSet.
     */
    public void enterInclusion (IASTInclusion arg0)
    {
      if (debug)
          System.out.println("made it to enterInclusion.....name = " +
                             arg0.getName());
      
      if (!ic.contains(arg0.getName())) {
        ic.add(arg0.getName());
        number_includes++;
      }

      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || !checkScope(arg0.getName(), line.getText()))
        return;
      
      if (debug)
          System.out.println(".....lineText = " + line.getText().trim());
      // See if there is already a tag for this include on this line
      if (line.getTag(0) != null)
        return;
      String lineText = line.getText();

      line.addTag(DOMTagTypes.KEYWORD, "#include", 0);
      
      int i = lineText.indexOf("<");
      int j = lineText.indexOf(">");
      if (i == -1) {
        i = lineText.indexOf('"');
        j = lineText.lastIndexOf('"');
      }
      if (i != -1)
        line.addTag(DOMTagTypes.INCLUDE, lineText.substring(i+1,j), i+1);
    }

    public void acceptMacro (IASTMacro arg0)
    {
      if (debug)
          System.out.println("made it to acceptMacro.....name = " +
                             arg0.getName());
      
      // Add macro to the list of defines
      if (!md.contains(arg0.getName())) {
      md.add(arg0.getName());
      }
      
      // If we are in the middle of an include, skip on out
      if (number_includes > 0)
        return;

      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      if (line == null || ! checkScope(arg0.getName(), line.getText()))
        return;
      
      String lineText = line.getText();
      
      if (debug)
          System.out.println(".....lineText = " + lineText.trim());

      // if this is not a #define stmt, we do not need the KEYWORD entry
      if (lineText.indexOf(DEFINE) >= 0)
        {
          line.addTag(DOMTagTypes.KEYWORD,
                      lineText.substring(0, arg0.getNameOffset()
                                            - line.getOffset()),
                      arg0.getStartingOffset() - line.getOffset());
          
        }
            
      line.addTag(
                  DOMTagTypes.MACRO, arg0.getName(),
                  arg0.getNameOffset() - line.getOffset());
    }

    /* UNIMPLEMENTED INTERFACE FUNCTIONS */
    public void acceptEnumeratorReference (IASTEnumeratorReference arg0)
    {
      if (debug)
        System.out.println("made it to acceptEnumeratorReference.....name = " + 
                           arg0.getName());
    }

    public void acceptEnumerationReference (IASTEnumerationReference arg0)
    {
      if (debug)
        System.out.println("made it to acceptEnumerationReference.....name = " +
                           arg0.getName());
    }

    public void acceptFriendDeclaration (IASTDeclaration arg0)
    {
      if (debug)
        System.out.println("made it to acceptFriendDeclaration....." + 
                           arg0.getClass().getName());
    }

    public void acceptASMDefinition (IASTASMDefinition arg0)
    {
      if (debug)
        System.out.println("made it to acceptASMDefinition.....name = " +
                           arg0.getClass().getName());
    }

    /* Probably not useful */
    public void enterCodeBlock (IASTCodeScope arg0)
    {
      if (debug)
        System.out.println("made it to enterCodeBlock.....name = " +
                           arg0.getClass().getName());
    }

    public void acceptElaboratedForewardDeclaration (
                                                     IASTElaboratedTypeSpecifier arg0)
    {
      if (debug)
        System.out.println("made it to acceptElaboratedForewardDeclaration....name = " +
                           arg0.getName());
    }

    public void exitFunctionBody (IASTFunction arg0)
    {
      if (quickparse)
        return;
      if (debug)
        System.out.println("exitFunctionBody....name = " + arg0.getName());
      
      DOMFunction func = source.getFunction(arg0.getName());
      if (func == null && debug)
        System.out.println("Could not get DOMFunction");
      DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
      DOMLine nameLine = source.getLineSpanningOffset(arg0.getNameOffset());
      
      if (line == null || !checkScope(arg0.getName(), nameLine.getText())
          || number_includes > 0)
          return;
      if (debug)
        {
          System.out.println(".....line = " + line.getText().trim());
          System.out.println(".....arg0.getEndingLine() = " + arg0.getEndingLine());
          System.out.println(".....func name = " + func.getName());
        }
      func.setEndingLine(arg0.getEndingLine());
      DOMLine line2 = source.getLine(arg0.getEndingLine());
      func.setEnd(line2.getOffset() + line.getLength());
      
    }

    public void exitCodeBlock (IASTCodeScope arg0)
    {
      if (debug)
        System.out.println("exitCodeBlock.....name = " + 
                           arg0.getClass().getName());
    }

    public void enterCompilationUnit (IASTCompilationUnit arg0)
    {
      if (debug)
        System.out.println("made it to enterCompilationUnit.....name = " +
                           arg0.getClass().getName());
    }

    public void enterLinkageSpecification (IASTLinkageSpecification arg0)
    {
      if (debug)
        System.out.println("made it to enterLinkageSpecification.....name = " +
                           arg0.getClass().getName());
    }

    public void exitMethodBody (IASTMethod arg0)
    {
      if (debug)
        System.out.println("made it to exitMethodBody.....name = " +
                           arg0.getName());
    }

    public void exitTemplateDeclaration (IASTTemplateDeclaration arg0)
    {
      if (debug)
        System.out.println("made it to exitTemplateDeclaration.....name = " +
                           arg0.getClass().getName());
    }

    public void exitTemplateSpecialization (IASTTemplateSpecialization arg0)
    {
      if (debug)
        System.out.println("made it to exitTemplateSpecialization.....name = " +
                           arg0.getClass().getName());
    }

    public void exitTemplateExplicitInstantiation (
                                                   IASTTemplateInstantiation arg0)
    {
      if (debug)
        System.out.println("made it to exitTemplateExplicitInstantiation.....name = " +
                           arg0.getClass().getName());
    }

    public void exitLinkageSpecification (IASTLinkageSpecification arg0)
    {
      if (debug)
        System.out.println("made it to exitLinkageSpecification.....name = " +
                           arg0.getClass().getName());
    }

    public void exitClassSpecifier (IASTClassSpecifier arg0)
    {
      if (debug)
        System.out.println("made it to exit Class Specifier.....name = " +
                           arg0.getName());
    }

    public void exitNamespaceDefinition (IASTNamespaceDefinition arg0)
    {
      if (debug)
        System.out.println("made it to exitNamespaceDefinition.....name = " +
                           arg0.getName());
    }

    public void exitInclusion (IASTInclusion arg0)
    {
      if (debug)
        System.out.println("made it to exitInclusion.....name = " +
                           arg0.getName());
      if (ic.contains(arg0.getName())) {
        ic.remove(arg0.getName());
        number_includes--;
      }
    }

    public void exitCompilationUnit (IASTCompilationUnit arg0)
    {
      if (debug)
        System.out.println("made it to exitCompilationUnit.....name = " +
                           arg0.getClass().getName());
    }

    public CodeReader createReader (String arg0, Iterator arg1)
    {
      File f1 = new File(arg0.toString());
      if (f1.exists())
        {
          try
            {
              CodeReader cr = new CodeReader(arg0.toString());
              return cr;
            }
          catch (IOException ex)
            {
              System.err.println("Cannot create FileInputStream for "
                                 + f1.toString());
            }
        }
      return null;
    }

    public boolean acceptProblem (IProblem arg0)
    {
      if (debug)
        System.out.println("Made it to acceptProblem" + ".....error = "
                           + arg0.getMessage() + ".....line # = "
                           + arg0.getSourceLineNumber() + ".....source start = "
                           + arg0.getSourceStart() + "\n"
                           + ".....originating class name = "
                           + arg0.getClass().getName()
                           + ".....arguments = " + arg0.getArguments());
      return false;
    }
    
    /**
     * checkScope tests whether or not the token we are parsing is in the
     *     source file we are parsing
     *     
     * @param token is the token passed back from the CDTParser
     * @param linetext is the line of text from the source file that
     *         triggered the callback
     *         
     * @return true if the token is found in the line, false if not
     */
    public boolean checkScope(String token, String linetext) 
    {
      if (linetext.indexOf(token) == -1) 
        return false;
      else
        return true;
    }
    
    /**
     * checkForMacro does a check to see if the line we are on contains a macro.
     * Due to the way the CDTParser handles macros, we are not notified when one
     * is found, only when one is defined. We must do the finding unfortunately
     * and this method does it.
     * 
     * @param Line is a DOMLine object of the line currently being parsed
     * 
     */
    private boolean checkForMacro(DOMLine line) {
      if (debug)
        System.out.println(".....made it to checkForMacro");
      String lineText = line.getText();
      Iterator it = md.iterator();
      while (it.hasNext()) {
        Object macro = it.next();
        String macro_name = (String) macro;
        int start_index = lineText.indexOf(macro_name);
        if (start_index >= 0) {
          // pluck the macro name out of the line
          int end_index = start_index + macro_name.length();
          // Make sure it is not part of another variable name
          char next_char = lineText.charAt(end_index);
          switch (next_char) {
            case ' ':
            case '(':
              line.addTag(DOMTagTypes.MACRO, macro_name, start_index);
              return true;
            default:
              return false;
          }
        }
      }
      return false;
    }
    
    /**
     * checkVariableIndex is a kludge to find the starting offset within a
     * line of a variable found on a line with a macro.  Unfortunately the
     * CDTParser has bugs that are difficult to find and has a lot of
     * things marked as "TODO", especially around methods used to handle
     * macros.  One bug is that the CDTParser passes back the incorrect
     * starting index of a variable/parameter on a line with a macro.  This
     * method finds the correct position and sends it back.
     * 
     * @param lineText is a String containing the text of the line being parsed
     * @param var is a String containing the variable/parameter name to be looked for
     */
    private int checkVariableIndex(String lineText, String var) {
      if (debug)
        System.out.println(".....made it to checkVariableIndex");
      int start = 0;
      while (start <= lineText.length()) {
        int start_index = lineText.indexOf(var, start);
        if (start_index == -1)
            return -1;
        int end_index = start_index + var.length() - 1;
        char next_char = lineText.charAt(end_index + 1);
        // Make sure the var string is not part of another longer variable name
        switch (next_char) {
          case ' ':
          case ',':
          case ')':
          case ';':
          case '>':
          case '<':
          case '=':
          case '[':
          case '.':
          case '+':
          case '-':
          case '(':
            return start_index;
          default:
            start = end_index + 1;
        }
      }
      return -1;
    }
  }
}
