# Fix unreferenced imports
s:\(import antlr.TreeParser;\):/* \1 */:
s:\(import antlr.Token;\):/* \1 */:
s:\(import antlr.ANTLRException;\):/* \1 */:
s:\(import antlr.MismatchedTokenException;\):/* \1 */:
s:\(import antlr.SemanticException;\):/* \1 */:
s:\(import antlr.collections.impl.BitSet;\):/* \1 */:
s:\(import antlr.ASTPair;\):/* \1 */:
s:\(import antlr.collections.impl.ASTArray;\):/* \1 */:

# Fix unread variables
/\<expr_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp60_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp61_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp62_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp63_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp64_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp65_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp66_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp67_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp68_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp69_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp70_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp71_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp72_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp73_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp74_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp75_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp76_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp77_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp78_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp79_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp80_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp81_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp82_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp83_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp84_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp85_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp86_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp87_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp88_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp89_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp90_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp91_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp92_AST_in\> =/ s:\(^.*$\):/* \1 */:
