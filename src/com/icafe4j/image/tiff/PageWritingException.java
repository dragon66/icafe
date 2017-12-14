/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.tiff;

import java.io.IOException;

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/14/2017
 */
public class PageWritingException extends IOException {

	private static final long serialVersionUID = 2704889922336376975L;

	public PageWritingException() {}

	public PageWritingException(String message) {
		super(message);
	}

	public PageWritingException(Throwable cause) {
		super(cause);
	}

	public PageWritingException(String message, Throwable cause) {
		super(message, cause);
	}	
}
