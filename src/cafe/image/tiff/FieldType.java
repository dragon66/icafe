/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.tiff;

import java.util.HashMap;
import java.util.Map;

/**
 * TIFF field type.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2013
 */
public enum FieldType {
	BYTE("Byte", (short)0x0001),
	ASCII("ASCII", (short)0x0002),
	SHORT("Short", (short)0x0003),
	LONG("Long", (short)0x0004),
	RATIONAL("Rational", (short)0x0005),
	SBYTE("SByte", (short)0x0006),
	UNDEFINED("Undefined", (short)0x0007),
	SSHORT("SShort", (short)0x0008),
	SLONG("SLong", (short)0x0009),
	SRATIONAL("SRational", (short)0x000a),
	FLOAT("Float", (short)0x000b),
	DOUBLE("Double", (short)0x000c),
	IFD("IFD", (short)0x000d),
	
	UNKNOWN("Unknown", (short)0x0000);
	
	private FieldType(String name, short value) {
		this.name = name;
		this.value = value;
	}	
	
	public String getName() {
		return name;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		return name;
	}
	
    public static FieldType fromShort(short value) {
       	FieldType fieldType = typeMap.get(value);
    	if (fieldType == null)
    	   return UNKNOWN;
   		return fieldType;
    }
    
    private static final Map<Short, FieldType> typeMap = new HashMap<Short, FieldType>();
       
    static
    {
      for(FieldType fieldType : values())
           typeMap.put(fieldType.getValue(), fieldType);
    } 
	
	private final String name;
	private final short value;
}