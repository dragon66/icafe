package com.icafe4j.scripting.lexer;

/*
 * STExample.java - An example using a Stream Tokenizer.
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

import java.util.*;
import java.io.IOException;
import java.io.StreamTokenizer;



/**
 * A simple interactive calculator. FieldType in the entries of the
 * form "<variable> = <expression>" or simple "= <expression>"
 * In the first case the value of the expression will be assigned
 * to the named variable, in the latter the value of the expression
 * is simply printed.
 *
 * Instead of an expression you can type the following commands:
 *           dump - dump out currently defined variables
 *          clear - clears all defined variables.
 *   quit or exit - exits the application.
 *           help - gives a brief help message.
 *
 */
public class STExample {
    static String helptext[] = {
        "Simple Interactive Calculator Application v1.0",
        "Copyright (c) 1996 by Chuck McManis, freely redistributable.",
        " ",
        "This is a simple interactive calculator, you type expressions",
        "into it. Expressions are of the form:",
        "    [varname] = expression <nl>",
        "If the variable name is omitted the value of the expression",
        "is simply printed. If present, the value is assigned to the",
        "variable named varname.",
        " ",
        "Available operators are +,-,/,*,&,|,#, and ^.",
        " ",
        "Other available commands are:",
        "    dump - display all defined variables.",
        "   clear - clear all defined variables.",
        "    help - display this message.",
        "    quit - exit the program."
    };

    static void help() {
        for (int i = 0; i < helptext.length; i++) {
            System.out.println(helptext[i]);
        }
    }

    static void dumpVariables(Hashtable<String, Double> varList) {
        Enumeration<String> e = varList.keys();
        System.out.println("Variable Dump:");
        while (e.hasMoreElements()) {
            String zz = e.nextElement();
            double d = varList.get(zz).doubleValue();
            System.out.println(zz+" = "+d);
        }
    }


    public static void main(String args[]) throws IOException {
        Hashtable<String, Double> variables = new Hashtable<String, Double>();
        @SuppressWarnings("deprecation")
		StreamTokenizer st = new StreamTokenizer(System.in);
        st.eolIsSignificant(true);
        st.lowerCaseMode(true);
        st.ordinaryChar('/');
        st.ordinaryChar('-');

        while (true) {
            Expression res;
            int c = StreamTokenizer.TT_EOL;
            String varName = null;

            System.out.println("Enter an expression...");
            try {
                while (true) {
                    c = st.nextToken();
                    if (c == StreamTokenizer.TT_EOF) {
                        System.exit(1);
                    } else if (c == StreamTokenizer.TT_EOL) {
                        continue;
                    } else if (c == StreamTokenizer.TT_WORD) {
                        if (st.sval.compareTo("dump") == 0) {
                            dumpVariables(variables);
                            continue;
                        } else if (st.sval.compareTo("clear") == 0) {
                            variables = new Hashtable<String, Double>();
                            continue;
                        } else if (st.sval.compareTo("quit") == 0) {
                            System.exit(0);
                        } else if (st.sval.compareTo("exit") == 0) {
                            System.exit(0);
                        } else if (st.sval.compareTo("help") == 0) {
                            help();
                            continue;
                        }
                        varName = st.sval;
                        c = st.nextToken();
                    }
                    break;
                }
                if (c != '=') {
                    throw new SyntaxError("missing initial '=' sign.");
                }
                res = ParseExpression.expression(st);
            } catch (SyntaxError se) {
                res = null;
                varName = null;
                System.out.println("\nSyntax Error detected! - "+se.getMsg());
                while (c != StreamTokenizer.TT_EOL)
                    c = st.nextToken();
                continue;
            }

            c = st.nextToken();
            if (c != StreamTokenizer.TT_EOL) {
                if (c == ')')
                    System.out.println("\nSyntax Error detected! - To many closing parens.");
                else
                    System.out.println("\nBogus token on input - "+c);
                while (c != StreamTokenizer.TT_EOL)
                    c = st.nextToken();
            } else {
                try {
                    Double z;
                    System.out.println("Parsed expression : "+res.unparse());
                    z = new Double(res.value(variables));
                    System.out.println("Value is : "+z);
                    if (varName != null) {
                        variables.put(varName, z);
                        System.out.println("Assigned to : "+varName);
                    }
                } catch (ExecError ee) {
                    System.out.println("Execution error, "+ee.getMsg()+"!");
                }
            }
        }
    }
}