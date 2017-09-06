package com.icafe4j.scripting.lexer;

/*
 * Expression.java - Parse and evaluate expressions for BASIC.
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

import java.util.Hashtable;



/**
 * This is the base class for the simple expressions.
 *
 * Expressions are parsed by the class <b>ParseExpression</b> which creates
 * a parse tree using objects of type expression. The subclasses
 * <b>ConstantExpression</b> and <b>VariableExpression</b>
 * hold specific types of indivisible elements.
 *
 * See the class ParseExpression for the grammar and precedence rules.
 */

class Expression {
    Expression arg1, arg2;
    int oper;

    /**
     * These are the valid operator types.
     */

    final static int OP_ADD  = 1;   // Addition '+'
    final static int OP_SUB  = 2;   // Subtraction '-'
    final static int OP_MUL  = 3;   // Multiplication '*'
    final static int OP_DIV  = 4;   // Division '/'
    final static int OP_EXP  = 5;   // Exponentiation '**'
    final static int OP_AND  = 6;   // Bitwise AND '&'
    final static int OP_IOR  = 7;   // Bitwise inclusive OR '|'
    final static int OP_XOR  = 8;   // Bitwise exclusive OR '^'
    final static int OP_NOT  = 9;   // Unary negation '!'
    final static int OP_NEG  = 10;  // Unary minus

    final static String opVals[] = {
        "<NULL>", "+", "-", "*", "/", "^", "&", "|", "#", "!", "-",
    };

    protected Expression() {
    }

    /**
     * Create a new expression.
     */
    protected Expression(int op, Expression a, Expression b) {
        arg1 = a;
        arg2 = b;
        oper = op;
    }

    /**
     * Create a unary expression.
     */
    protected Expression(int op, Expression a) {
        arg2 = a;
        oper = op;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("(");
        if (arg1 != null)
            sb.append(arg1.toString());
        sb.append(opVals[oper]+arg2.toString()+")");
        return sb.toString();
    }

    String unparse() {
        if (arg1 == null) {
            return opVals[oper]+arg2.unparse();
        }
        return "("+arg1.unparse()+" "+opVals[oper]+" "+arg2.unparse()+")";
    }


    /**
     * This method evaluates the expression in the context of the
     * passed in program. It throws runtime errors for things like
     * no such variable and divide by zero.
     *
     * Note that for boolean operations the value 1.0 == true and
     * the value 0.0 is equivalent to false.
     */
    double value(Hashtable<String, Double> vars) throws ExecError {
        switch (oper) {
            case OP_ADD :
                return arg1.value(vars) + arg2.value(vars);

            case OP_SUB :
                return arg1.value(vars) - arg2.value(vars);

            case OP_MUL :
                return arg1.value(vars) * arg2.value(vars);

            case OP_DIV :
                if (arg2.value(vars) == 0) {
                    throw new ExecError("divide by zero!");
                }
                return arg1.value(vars) / arg2.value(vars);

            case OP_XOR :
                return ((long) arg1.value(vars)) ^ ((long) arg2.value(vars));

            case OP_IOR :
                return ((long) arg1.value(vars)) | ((long) arg2.value(vars));

            case OP_AND :
                return ((long) arg1.value(vars)) & ((long) arg2.value(vars));

            case OP_EXP :
                return (Math.pow(arg1.value(vars), arg2.value(vars)));

            case OP_NOT :
                return ~((long)(arg2.value(vars)));

            case OP_NEG :
                return 0 - arg2.value(vars);

            default:
                throw new ExecError("Illegal operator in expression!");

        }
    }
}
