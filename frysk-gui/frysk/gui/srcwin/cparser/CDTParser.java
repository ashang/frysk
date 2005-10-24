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

import frysk.gui.srcwin.SourceBuffer;
import frysk.gui.srcwin.StaticParser;
import frysk.gui.srcwin.Variable;

/**
 * @author ajocksch
 *
 */
public class CDTParser implements StaticParser {

	private SourceBuffer buffer;
	
	/* (non-Javadoc)
	 * @see frysk.gui.srcwin.StaticParser#parse(java.lang.String, frysk.gui.srcwin.SourceBuffer)
	 */
	public void parse(String filename, SourceBuffer buffer) throws IOException {
		this.buffer = buffer;
		
		ParserCallBack callback = new ParserCallBack();
		IParser parser = ParserFactory.createParser(
				ParserFactory.createScanner(filename, new ScannerInfo(), ParserMode.COMPLETE_PARSE,
						ParserLanguage.CPP, callback, new NullLogService(), null),
				callback,
				ParserMode.COMPLETE_PARSE,
				ParserLanguage.CPP,
				new NullLogService());
		
		if(!parser.parse())
			System.err.println("Some errors found during parse");
	}

	
	class ParserCallBack implements ISourceElementRequestor{

		public void acceptVariable(IASTVariable arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addVariable(new Variable(arg0.getName(), arg0.getStartingLine(), arg0.getNameOffset(), false));
		}

		public void acceptFunctionDeclaration(IASTFunction arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addFunction(arg0.getName(), arg0.getNameOffset(), true);
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				buffer.addLiteral(param.getStartingOffset(), param.getNameOffset() - param.getStartingOffset());
				buffer.addVariable(new Variable(param.getName(), param.getStartingLine(), param.getNameOffset(), false));
			}
		}
		
		public void enterFunctionBody(IASTFunction arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addFunction(arg0.getName(), arg0.getNameOffset(), true);
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				buffer.addLiteral(param.getStartingOffset(), param.getNameOffset() - param.getStartingOffset());
				buffer.addVariable(new Variable(param.getName(), param.getStartingLine(), param.getNameOffset(), false));
			}
		}
		
		public void acceptTypedefDeclaration(IASTTypedefDeclaration arg0) {}

		public void acceptEnumerationSpecifier(IASTEnumerationSpecifier arg0) {}	

		public void enterNamespaceDefinition(IASTNamespaceDefinition arg0) {}

		public void enterClassSpecifier(IASTClassSpecifier arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addClass(arg0.getNameOffset(), arg0.getName().length());
		}	

		public void acceptField(IASTField arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addVariable(new Variable(arg0.getName(), arg0.getStartingLine(), arg0.getNameOffset(), true));
		}

		public void acceptClassReference(IASTClassReference arg0) {
			buffer.addClass(arg0.getOffset(), arg0.getName().length());
		}

		public void acceptTypedefReference(IASTTypedefReference arg0) {}

		public void acceptVariableReference(IASTVariableReference arg0) {
			buffer.addVariable(new Variable(arg0.getName(), 0, arg0.getOffset(), false));
		}

		public void acceptFunctionReference(IASTFunctionReference arg0) {
			buffer.addFunction(arg0.getName(), arg0.getOffset(), false);
		}

		public void acceptFieldReference(IASTFieldReference arg0) {}

		public void acceptParameterReference(IASTParameterReference arg0) {
			buffer.addVariable(new Variable(arg0.getName(), 0, arg0.getOffset(), false));			
		}
		
		public void acceptAbstractTypeSpecDeclaration(IASTAbstractTypeSpecifierDeclaration arg0) {
			buffer.addLiteral(arg0.getNameOffset(), arg0.getName().length());
		}

		public void acceptMethodDeclaration(IASTMethod arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addFunction(arg0.getName(), arg0.getNameOffset(), true);
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				int nameOffset = param.getNameOffset();
				if(nameOffset != -1){
					buffer.addLiteral(param.getStartingOffset(), param.getNameOffset() - param.getStartingOffset());
					buffer.addVariable(new Variable(param.getName(), param.getStartingLine(), param.getNameOffset(), false));
				}
				else{
					// Figure out how to do function declarations of type "foo(int)" here
				}
			}
		}

		public void acceptMethodReference(IASTMethodReference arg0) {
			buffer.addFunction(arg0.getName(), arg0.getOffset(), false);
		}
		
		public void enterMethodBody(IASTMethod arg0) {
			buffer.addLiteral(arg0.getStartingOffset(), arg0.getNameOffset() - arg0.getStartingOffset());
			buffer.addFunction(arg0.getName(), arg0.getNameOffset(), true);
			Iterator iter = arg0.getParameters();
			while(iter.hasNext()){
				IASTParameterDeclaration param = (IASTParameterDeclaration) iter.next();
				buffer.addLiteral(param.getStartingOffset(), param.getNameOffset() - param.getStartingOffset());
				buffer.addVariable(new Variable(param.getName(), param.getStartingLine(), param.getNameOffset(), false));
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
