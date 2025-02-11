/*
Copyright (c) 2011+, HL7, Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this 
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
   this list of conditions and the following disclaimer in the documentation 
   and/or other materials provided with the distribution.
 * Neither the name of HL7 nor the names of its contributors may be used to 
   endorse or promote products derived from this software without specific 
   prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

 */
/**
 * 
 */
package org.hl7.fhir.r5.model;

/*-
 * #%L
 * org.hl7.fhir.r5
 * %%
 * Copyright (C) 2014 - 2019 Health Level 7
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

/**
 * Primitive type "integer" in FHIR: A signed 32-bit integer
 */
@DatatypeDef(name = "integer")
public class Integer64Type extends PrimitiveType<Long> /* implements IBaseInteger64Datatype */ {

	private static final long serialVersionUID = 3L;

	/**
	 * Constructor
	 */
	public Integer64Type() {
		// nothing
	}

	/**
	 * Constructor
	 */
	public Integer64Type(long theInteger) {
		setValue(theInteger);
	}

	/**
	 * Constructor
	 * 
	 * @param theIntegerAsString
	 *            A string representation of an integer
	 * @throws IllegalArgumentException
	 *             If the string is not a valid integer representation
	 */
	public Integer64Type(String theIntegerAsString) {
		setValueAsString(theIntegerAsString);
	}

	/**
	 * Constructor
	 * 
	 * @param theValue The value
	 * @throws IllegalArgumentException If the value is too large to fit in a signed integer
	 */
	public Integer64Type(Long theValue) {
	    if (theValue < java.lang.Long.MIN_VALUE || theValue > java.lang.Long.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (theValue + " cannot be cast to int without changing its value.");
	    }
	    if(theValue!=null) {
	    	setValue((long)theValue.longValue());
	    }
	}

	@Override
	protected Long parse(String theValue) {
		try {
			return Long.parseLong(theValue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	protected String encode(Long theValue) {
		return Long.toString(theValue);
	}

	@Override
	public Integer64Type copy() {
		Integer64Type ret = new Integer64Type(getValue());
    copyValues(ret);
    return ret;
	}

	public String fhirType() {
		return "integer64";		
	}
}
