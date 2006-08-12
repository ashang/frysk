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
/\<tmp42_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp43_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp44_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp45_AST\> = / s:\(^.*$\):/* \1 */:
/\<tmp60_AST\> = / s:\(^.*$\):/* \1 */:

/buildTokenTypeASTClassMap/,+2 s/};/}/

