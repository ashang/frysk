s:\(import antlr.TokenStreamIOException;\):/* \1 */:
s:\(import antlr.ANTLRException;\):/* \1 */:
s:\(import antlr.LLkParser;\):/* \1 */:
s:\(import antlr.MismatchedTokenException;\):/* \1 */:
s:\(import antlr.SemanticException;\):/* \1 */:
s:\(import java.util.Hashtable;\):/* \1 */:
s:\(import frysk.lang.*;\):/* \1 */:
s:\(import java.util.\*;\):/* \1 */:
1538 s:};:}:

75 s:\(private String sInputExpression;\):/* \1 */:
81 s:\(sInputExpression = sInput;\):/* \1 */:

417 s:\(AST ques_AST = null;\):/* \1 */:
430 s:\(ques_AST =\):/* \1 */:

420 s:\(AST colon_AST = null;\):/* \1 */:
435 s:\(colon_AST =\):/* \1 */:

981 s:\(AST post_expr2_AST = null;\):/* \1 */:
1029 s:\(post_expr2_AST = .AST.returnAST;\):/* \1 */:

1090 s:\(AST tmp[0-9][0-9]*_AST = null;\):/* \1 */:
1091 s:\(tmp[0-9][0-9]*_AST =\):/* \1 */:

1095 s:\(AST tmp[0-9][0-9]*_AST = null;\):/* \1 */:
1096 s:\(tmp[0-9][0-9]*_AST =\):/* \1 */:

1105 s:\(AST tmp[0-9][0-9]*_AST = null;\):/* \1 */:
1106 s:\(tmp[0-9][0-9]*_AST =\):/* \1 */:

1138 s:\(AST tmp[0-9][0-9]*_AST = null;\):/* \1 */:
1139 s:\(tmp[0-9][0-9]*_AST =\):/* \1 */:

/buildTokenTypeASTClassMap/,+2 s/};/}/

# Causes "unread" warnings from ecj
/post_expr2_AST/d
/tmp42_AST/d
/tmp43_AST/d
/tmp44_AST/d
/tmp45_AST/d
