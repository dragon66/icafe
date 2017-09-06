package com.icafe4j.scripting.lexer;

/*
 * ConstantExpression.java - An expression which is simply a constant.
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
 * This class implements the simplest possible expression, a constant.
 */
class ConstantExpression extends Expression {
    private double v;

    ConstantExpression(double a) {
        super();
        v = a;
    }

    double value(Hashtable<String, Double> pgm) throws ExecError { return v; }

    String unparse() { return ""+v; }

    public String toString() {
        return "ConstantExpression : "+v;
    }
}
