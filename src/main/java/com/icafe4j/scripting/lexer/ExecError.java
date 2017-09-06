package com.icafe4j.scripting.lexer;

/*
 * ExecError.java - Execution Error in our expression parser.
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
 * When an error occurs while evaluating the expression we throw an
 * ExecError.
 */
public class ExecError extends Exception {
  	private static final long serialVersionUID = 5987770861095572930L;
	private String msg = "none";
    public ExecError() {
        super();
    }

    public ExecError(String s) {
        super(s);
        msg = s;
    }

    public String getMsg() { return msg; }
}
