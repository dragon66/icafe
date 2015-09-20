package com.icafe4j.scripting.lexer;

/*
 * SyntaxError.java - Syntax Error in our expression parser.
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

/**
 * When an error occurs while parsing the expression we throw a
 * SyntaxError.
 */
public class SyntaxError extends Exception {
 	private static final long serialVersionUID = -3874918297984265806L;

 	String msg = "<NONE>";
    public SyntaxError() {
        super();
    }

    public SyntaxError(String s) {
        super(s);
        msg = s;
    }

    String getMsg() { return msg; }
}
