# Fix unreferenced imports
s:\(import antlr.TokenStreamIOException;\):/* \1 */:
s:\(import antlr.ANTLRException;\):/* \1 */:
s:\(import antlr.LLkParser;\):/* \1 */:
s:\(import antlr.MismatchedTokenException;\):/* \1 */:
s:\(import antlr.SemanticException;\):/* \1 */:
s:\(import java.util.Hashtable;\):/* \1 */:
s:\(import frysk.value.*;\):/* \1 */:
s:\(import java.util.\*;\):/* \1 */:

# Fix for gcj
/buildTokenTypeASTClassMap/,+2 s/};/}/

