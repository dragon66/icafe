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

package com.icafe4j.util.zip;

/**
 * Interface to be implemented by CRC32 and Adler32 etc.
 */
public interface Checksum
{  
  public long getValue();
  
  public void update(int b);
 
  public void update(byte[] b, int offset, int length);
 
  public void reset();
}