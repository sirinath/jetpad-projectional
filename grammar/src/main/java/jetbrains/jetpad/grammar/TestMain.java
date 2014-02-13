package jetbrains.jetpad.grammar;

import jetbrains.jetpad.grammar.lr1.LR1TableGenerator;
import jetbrains.jetpad.grammar.slr.SLRTableGenerator;

public class TestMain {
  public static void main(String[] args) {
    testTestGrammar();
  }

  private static void testFunGrammar() {
    Grammar g = new Grammar();

    NonTerminal e = g.newNonTerminal("E");
    g.newRule(g.getStart(), e);

    Terminal lp = g.newTerminal("(");
    Terminal rp = g.newTerminal(")");
    Terminal colon = g.newTerminal(":");
    Terminal arrow = g.newTerminal("->");
    Terminal id = g.newTerminal("id");


    g.newRule(e, e, e).setPriority(10).setAssociativity(Associativity.LEFT);
    g.newRule(e, e, arrow, e).setPriority(5).setAssociativity(Associativity.LEFT);
//    g.newRule(e, lp, id, colon, e, rp, arrow, e);
//    g.newRule(e, lp, e, rp);
    g.newRule(e, id);


    System.out.println("LR1");

    LR1TableGenerator lr1gen = new LR1TableGenerator(g);
    lr1gen.dumpTable();
  }


  private static void testTestGrammar() {
    Grammar g = new Grammar();

    Terminal a = g.newTerminal("a");
    Terminal b = g.newTerminal("b");
    Terminal c = g.newTerminal("c");
    Terminal d = g.newTerminal("d");

    NonTerminal ss = g.newNonTerminal("SS");
    NonTerminal aa = g.newNonTerminal("A");

    g.newRule(g.getStart(), ss);
    g.newRule(ss, aa, a);
    g.newRule(ss, b, aa, c);
    g.newRule(ss, d, c);
    g.newRule(ss, b, d, a);
    g.newRule(aa, d);

    new LR1TableGenerator(g).dumpTable();
//
//    new SLRTableGenerator(g).dumpTable();
  }

//
//  public static void main(String[] args) {
//    Grammar g = new Grammar();
//
//    NonTerminal s = g.getStart();
//    NonTerminal e = g.newNonTerminal("e");
//    NonTerminal t = g.newNonTerminal("t");
//
//    Terminal minus = g.newTerminal("-");
//    Terminal n = g.newTerminal("n");
//    Terminal lp = g.newTerminal("(");
//    Terminal rp = g.newTerminal(")");
//
//
//    g.newRule(s, e);
//    g.newRule(e, e, minus, t);
//    g.newRule(e, t);
//    g.newRule(t, n);
//    g.newRule(t, lp, e, rp);
//
//
//    new LR1TableGenerator(g).dumpTable();
//  }
}