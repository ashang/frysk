# Fix unreferenced imports
s:\(import antlr.TokenStreamIOException;\):/* \1 */:
s:\(import antlr.ANTLRException;\):/* \1 */:
s:\(import antlr.LLkParser;\):/* \1 */:
s:\(import antlr.MismatchedTokenException;\):/* \1 */:
s:\(import antlr.SemanticException;\):/* \1 */:
s:\(import java.util.Hashtable;\):/* \1 */:
s:\(import frysk.lang.*;\):/* \1 */:
s:\(import java.util.\*;\):/* \1 */:

# Fix unread variables
/\<sInputExpression\>/ s:\(^.*$\):/* \1 */:
/\<post_expr2_AST\> =/ s:\(^.*$\):/* \1 */:
/\<ques_AST\> =/ s:\(^.*$\):/* \1 */:
/\<colon_AST\> =/ s:\(^.*$\):/* \1 */:
/\<colon\> = / s:\(^.*$\):/* \1 */:
/\<tmp48_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp49_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp58_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp59_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp60_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp61_AST\> = / s:\(^.*$\):/* \1 */:

/buildTokenTypeASTClassMap/,+2 s/};/}/

