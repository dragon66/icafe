/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.tiff;

/**
 * UnsupportedCompressionException.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/22/2017
 */
public class UnsupportedCompressionException extends RuntimeException {

	private static final long serialVersionUID = 4872531155771023673L;

	public UnsupportedCompressionException() {}

	public UnsupportedCompressionException(String message) {
		super(message);
	}

	public UnsupportedCompressionException(Throwable cause) {
		super(cause);
	}

	public UnsupportedCompressionException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedCompressionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
