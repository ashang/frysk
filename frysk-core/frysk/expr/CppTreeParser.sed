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
/\<primitive_type_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<expr_AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp6._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp7._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp8._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp9._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp10._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp11._AST_in\> =/ s:\(^.*$\):/* \1 */:
/\<tmp12._AST_in\> =/ s:\(^.*$\):/* \1 */:
