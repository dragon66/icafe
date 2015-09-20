package com.icafe4j.scripting.lexer;

/*
 * VariableExpression.java - An expression consisting of a variable.
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
 * This class implements an expression that is simply a variable. Or
 * more correctly the value of that variable.
 */
class VariableExpression extends Expression {
    private String v;

    VariableExpression(String a) {
        super();
        v = a;
    }

    double value(Hashtable<String, Double> pgm) throws ExecError {
        Double dd = pgm.get(v);
        if (dd == null)
            throw new ExecError(v+" is undefined");
        return (dd.doubleValue());
    }

    String unparse() { return v; }

    public String toString() { return "VariableExpression: "+v; }
}
