package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id: ZeroOrMoreBlock.java,v 1.1.1.1 2005/11/25 22:29:28 cagney Exp $
 */

class ZeroOrMoreBlock extends BlockWithImpliedExitPath {

    public ZeroOrMoreBlock(Grammar g) {
        super(g);
    }

    public ZeroOrMoreBlock(Grammar g, Token start) {
        super(g, start);
    }

    public void generate() {
        grammar.generator.gen(this);
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public String toString() {
        return super.toString() + "*";
    }
}
