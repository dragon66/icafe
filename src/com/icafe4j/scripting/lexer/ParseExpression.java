package com.icafe4j.scripting.lexer;

/*
 * ParseExpression.java - Parse an expression.
 *
 * Copyright (c) 1996 Chuck McManis, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * CHUCK MCMANIS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. CHUCK MCMANIS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

import java.io.StreamTokenizer;
import java.io.IOException;



/**
 * This class implements a simple recursive-descent parser for the
 * expression grammar I have designed. As an exercise it is right out
 * of Aho and Ullman's compiler design text.
 *
 * This grammar is defined with some nonterminals that allow us to embed the
 * precedence relationship of operators into the grammar. The grammar is
 * defined as follows:
 *
 * ELEMENT    ::=   id
 *             |    constant
 *             |    "(" expression ")"
 * PRIMARY    ::=   "-" ELEMENT
 *             |    "!" ELEMENT
 *             |    ELEMENT
 * FACTOR     ::=   PRIMARY "^" FACTOR
 *             |    PRIMARY
 * TERM       ::=   TERM "*" FACTOR
 *             |    TERM "/" FACTOR
 *             |    FACTOR
 * SUM        ::=   SUM "+" TERM
 *             |    SUM "-" TERM
 *             |    TERM
 * EXPRESSION ::=   EXPRESSION "&" SUM
 *             |    EXPRESSION "#" SUM
 *             |    EXPRESSION "|" SUM
 *             |    SUM
 *
 * Precidence rules from lowest to highest :
 *  1.  &, |, ^
 *  2.  +, -
 *  3.  *, /
 *  4.  **
 *  5.  unary -, unary !
 *
 */
class ParseExpression extends Expression {

    static Expression element(StreamTokenizer st) throws SyntaxError {
        Expression result = null;

        try {
            switch (st.nextToken()) {
                case StreamTokenizer.TT_NUMBER :
                    result = new ConstantExpression(st.nval);
                    break;

                case StreamTokenizer.TT_WORD :
                    result = new VariableExpression(st.sval);
                    break;
                case '(' :
                    result = expression(st);
                    st.nextToken();
                    if (st.ttype != ')') {
                        st.pushBack();
                        throw new SyntaxError("Mismatched parenthesis.");
                    }
                    break;
                default:
                    st.pushBack();
                    throw new SyntaxError("Unexpected symbol on input.");
            }
        } catch (IOException ioe) {
            throw new SyntaxError("Caught an I/O exception.");
        }
        return result;
    }

    static Expression primary(StreamTokenizer st) throws SyntaxError {
        try {
            switch (st.nextToken()) {
                case '!' :
                    return new Expression(OP_NOT, primary(st));
                case '-' :
                    return new Expression(OP_NEG, primary(st));
                default:
                    st.pushBack();
                    return element(st);
            }
        } catch (IOException ioe) {
            throw new SyntaxError("Caught an I/O Exception.");
        }
    }

    static Expression factor(StreamTokenizer st) throws SyntaxError {
        Expression result;

        result = primary(st);
        try {
            switch (st.nextToken()) {
                case '^':
                    result = new Expression(OP_EXP, result, factor(st));
                    break;
                default:
                    st.pushBack();
                    break;
            }
        } catch (IOException ioe) {
            throw new SyntaxError("Caught an I/O Exception.");
        }
        return result;
    }

    static Expression term(StreamTokenizer st) throws SyntaxError {
        Expression result;
        boolean done = false;

        result = factor(st);
        while (! done) {
            try {
                switch (st.nextToken()) {
                    case '*' :
                        result = new Expression(OP_MUL, result, factor(st));
                        break;
                    case '/' :
                        result = new Expression(OP_DIV, result, factor(st));
                        break;
                    default :
                        st.pushBack();
                        done = true;
                        break;
                }
            } catch (IOException ioe) {
                throw new SyntaxError("Caught an I/O exception.");
            }
        }
        return result;
    }

    static Expression sum(StreamTokenizer st) throws SyntaxError {
        Expression result;
        boolean done = false;

        result = term(st);

        while (! done) {
            try {
                switch (st.nextToken()) {
                    case '+':
                        result = new Expression(OP_ADD, result, term(st));
                        break;
                    case '-':
                        result = new Expression(OP_SUB, result, term(st));
                        break;
                    default :
                        st.pushBack();
                        done = true;
                        break;
                }
            } catch (IOException ioe) {
                throw new SyntaxError("Caught an I/O Exception.");
            }
        }
        return result;
    }

    static Expression expression(StreamTokenizer st) throws SyntaxError {
        Expression result;
        boolean done = false;

        result = sum(st);
        while (! done) {
            try {
                switch (st.nextToken()) {
                    case '&' :
                        result = new Expression(OP_AND, result, sum(st));
                        break;
                    case '|' :
                        result = new Expression(OP_IOR, result, sum(st));
                        break;
                    case '#' :
                        result = new Expression(OP_XOR, result, sum(st));
                        break;
                    default:
                        done = true;
                        st.pushBack();
                        break;
                }
            } catch (IOException ioe) {
                throw new SyntaxError("Got an I/O Exception.");
            }
        }
        return result;
    }

}
