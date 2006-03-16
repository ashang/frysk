/**
 * CDTParser uses the parser from the Eclipse CDT to generate static information
 * about the source file
 */
package frysk.gui.srcwin.cparser;

import java.io.IOException;
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

import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.dom.DOMTagTypes;
import frysk.gui.srcwin.SourceBuffer;
import frysk.gui.srcwin.StaticParser;

/**
 * @author ajocksch
 *
 */
public class CDTParser implements StaticParser {

	private DOMSource source;
	
	/* (non-Javadoc)
	 * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String, frysk.gui.srcwin.SourceBuffer)
	 */
	public void parse(DOMSource source, SourceBuffer buffer) throws IOException {
		this.source = source;
		
		String filename = source.getFilePath() + "/" + source.getFileName();
		
		ParserCallBack callback = new ParserCallBack();
		IParser parser = ParserFactory.createParser(
				ParserFactory.createScanner(filename,
						new ScannerInfo(), ParserMode.QUICK_PARSE,
						ParserLanguage.CPP, callback, new NullLogService(), null),
				callback,
				ParserMode.QUICK_PARSE,
				ParserLanguage.CPP,
				new NullLogService());
		
		if(!parser.parse())
			System.err.println("Some errors found during parse");
		
		/*
		 * The CDT Parser does not parse out comments for some reason,
		 * do a second parsing run and pick them out
		 */
		Tokenizer tokenMaker = new Tokenizer(filename);
		
		while(tokenMaker.hasMoreTokens()){
			Token t = tokenMaker.nextToken();
			
			// C++ style comments
			if(t.text.equals("//")){
				Token t2 = t;
				while(tokenMaker.hasMoreTokens() && tokenMaker.peek().lineNum == t.lineNum){
					t2 = tokenMaker.nextToken();
				}
				buffer.addComment(t.lineNum, t.colNum, t.lineNum, t2.colNum+t2.text.length());
			}
			// C Style comments
			else if(t.text.equals("/*")){
				Token t2 = t;
				while(tokenMaker.hasMoreTokens() && !tokenMaker.peek().text.equals("*/")){
					t2 = tokenMaker.nextToken();
				}
				t2 = tokenMaker.nextToken();
				buffer.addComment(t.lineNum, t.colNum, t2.lineNum, t2.colNum+t2.text.length());
			}
			// For some reason the CDTParser doesn't pick up this keyword either
			else if(t.text.equals("return")){
				DOMLine line = this.source.getLine(t.lineNum + 1);
				if(line == null)
					return;
				
				line.addTag(DOMTagTypes.KEYWORD, t.text, t.colNum);
			}
		}
	}

	
	class ParserCallBack implements ISourceElementRequestor{

		public void acceptVariable(IASTVariable arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());
		}

		public void acceptFunctionDeclaration(IASTFunction arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.FUNCTION, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());

			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
			
				line.addTag(DOMTagTypes.KEYWORD, lineText.substring(param.getStartingOffset() - line.getOffset(), param.getNameOffset() - line.getOffset()), param.getStartingOffset() - line.getOffset());
				line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(param.getNameOffset() - line.getOffset(), param.getNameOffset() - line.getOffset() + param.getName().length()), param.getNameOffset() - line.getOffset());
			}
		}
		
		public void enterFunctionBody(IASTFunction arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.FUNCTION, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());
			
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				
				line.addTag(DOMTagTypes.KEYWORD, lineText.substring(param.getStartingOffset() - line.getOffset(), param.getNameOffset() - line.getOffset()), param.getStartingOffset() - line.getOffset());
				line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(param.getNameOffset() - line.getOffset(), param.getNameOffset() - line.getOffset() + param.getName().length()), param.getNameOffset() - line.getOffset());
			}
		}
		
		public void acceptTypedefDeclaration(IASTTypedefDeclaration arg0) {}

		public void acceptEnumerationSpecifier(IASTEnumerationSpecifier arg0) {}	

		public void enterNamespaceDefinition(IASTNamespaceDefinition arg0) {}

		public void enterClassSpecifier(IASTClassSpecifier arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.CLASS_DECL, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());
		}	

		public void acceptField(IASTField arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());
		}

		public void acceptClassReference(IASTClassReference arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.CLASS_DECL, arg0.getName(), arg0.getOffset() - line.getOffset());
		}

		public void acceptTypedefReference(IASTTypedefReference arg0) {}

		public void acceptVariableReference(IASTVariableReference arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), arg0.getOffset() - line.getOffset());
		}

		public void acceptFunctionReference(IASTFunctionReference arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.FUNCTION, arg0.getName(), arg0.getOffset() - line.getOffset());
		}

		public void acceptFieldReference(IASTFieldReference arg0) {}

		public void acceptParameterReference(IASTParameterReference arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), arg0.getOffset() - line.getOffset());			
		}
		
		public void acceptAbstractTypeSpecDeclaration(IASTAbstractTypeSpecifierDeclaration arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.LOCAL_VAR, arg0.getName(), arg0.getStartingOffset() - line.getOffset());
		}

		public void acceptMethodDeclaration(IASTMethod arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.FUNCTION, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());

			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				int nameOffset = param.getNameOffset();
				if(nameOffset != -1){
					line.addTag(DOMTagTypes.KEYWORD, lineText.substring(param.getStartingOffset() - line.getOffset(), param.getNameOffset() - line.getOffset()), param.getStartingOffset() - line.getOffset());
					line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(param.getNameOffset() - line.getOffset(), param.getNameOffset() - line.getOffset() + param.getName().length()), param.getNameOffset() - line.getOffset());
				}
				else{
					// Figure out how to do function declarations of type "foo(int)" here
				}
			}
		}

		public void acceptMethodReference(IASTMethodReference arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getOffset());
			if(line == null)
				return;

			line.addTag(DOMTagTypes.FUNCTION, arg0.getName(), arg0.getOffset() - line.getOffset());
		}
		
		public void enterMethodBody(IASTMethod arg0) {
			DOMLine line = source.getLineSpanningOffset(arg0.getStartingOffset());
			if(line == null)
				return;
			
			String lineText = line.getText();
			
			line.addTag(DOMTagTypes.KEYWORD, lineText.substring(arg0.getStartingOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset()), arg0.getStartingOffset() - line.getOffset());
			line.addTag(DOMTagTypes.FUNCTION, lineText.substring(arg0.getNameOffset() - line.getOffset(), arg0.getNameOffset() - line.getOffset() + arg0.getName().length()), arg0.getNameOffset() - line.getOffset());
			
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				
				line.addTag(DOMTagTypes.KEYWORD, lineText.substring(param.getStartingOffset() - line.getOffset(), param.getNameOffset() - line.getOffset()), param.getStartingOffset() - line.getOffset());
				line.addTag(DOMTagTypes.LOCAL_VAR, lineText.substring(param.getNameOffset() - line.getOffset(), param.getNameOffset() - line.getOffset() + param.getName().length()), param.getNameOffset() - line.getOffset());
			}
		}
		
		/* UNIMPLEMENTED INTERFACE FUNCTIIONS */
		public void enterCodeBlock(IASTCodeScope arg0) {}
		public void acceptMacro(IASTMacro arg0) {}
		public void acceptUsingDirective(IASTUsingDirective arg0) {}
		public void acceptUsingDeclaration(IASTUsingDeclaration arg0) {}
		public void acceptASMDefinition(IASTASMDefinition arg0) {}
		public void acceptElaboratedForewardDeclaration(IASTElaboratedTypeSpecifier arg0) {}
		public void exitFunctionBody(IASTFunction arg0) {}
		public void exitCodeBlock(IASTCodeScope arg0) {}
		public void enterCompilationUnit(IASTCompilationUnit arg0) {}
		public void enterInclusion(IASTInclusion arg0) {}
		public void enterLinkageSpecification(IASTLinkageSpecification arg0) {}
		public void enterTemplateDeclaration(IASTTemplateDeclaration arg0) {}
		public void enterTemplateSpecialization(IASTTemplateSpecialization arg0) {}
		public void exitMethodBody(IASTMethod arg0) {}
		public void enterTemplateInstantiation(IASTTemplateInstantiation arg0) {}
		public void acceptTemplateParameterReference(IASTTemplateParameterReference arg0) {}
		public void acceptEnumeratorReference(IASTEnumeratorReference arg0) {}
		public void acceptNamespaceReference(IASTNamespaceReference arg0) {}
		public void acceptEnumerationReference(IASTEnumerationReference arg0) {}
		public void acceptFriendDeclaration(IASTDeclaration arg0) {}
		public void exitTemplateDeclaration(IASTTemplateDeclaration arg0) {}
		public void exitTemplateSpecialization(IASTTemplateSpecialization arg0) {}
		public void exitTemplateExplicitInstantiation(IASTTemplateInstantiation arg0) {}
		public void exitLinkageSpecification(IASTLinkageSpecification arg0) {}
		public void exitClassSpecifier(IASTClassSpecifier arg0) {}
		public void exitNamespaceDefinition(IASTNamespaceDefinition arg0) {}
		public void exitInclusion(IASTInclusion arg0) {}
		public void exitCompilationUnit(IASTCompilationUnit arg0) {}
		public CodeReader createReader(String arg0, Iterator arg1) {
			return null;
		}
		public boolean acceptProblem(IProblem arg0) {
			return false;
		}
	}
}
