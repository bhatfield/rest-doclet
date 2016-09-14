/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.restDoclet.docElements;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.restDoclet.constants.RestDocConstants.DocAnnotationTypes;
import org.cloudifysource.restDoclet.generation.Utils;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;
import com.sun.tools.javadoc.AnnotationDescImpl;

/**
 * 
 * @author yael
 *
 */
public class DocAnnotation {
	private final String name;
	private final Map<String, Object> attributes;

	public DocAnnotation(final String name) {
		this.name = name;
		attributes = new HashMap<String, Object>();
	}

	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name .
	 * @return The name without . or ().
	 */
	protected static String getShortName(final String name) {
		int beginIndex = name.lastIndexOf('.') + 1;
		int endIndex = name.lastIndexOf("()");
		if (endIndex == -1) {
			endIndex = name.length();
		}
		return name.substring(beginIndex, endIndex);
	}

	/**
	 * 
	 * @param value .
	 * @return Construct the value.
	 */
	public static Object constructAttrValue(final Object value) {
		if (value.getClass().isArray()) {
			AnnotationValue[] values = (AnnotationValue[]) value;
			Object firstValue = values[0].value();
			Object constractedValues = null;
			if (firstValue instanceof AnnotationDescImpl) {
				Class<?> annotationClass = 
						DocAnnotationTypes.getAnnotationClass(
								((AnnotationDescImpl) firstValue).annotationType().typeName());
				constractedValues = Array.newInstance(annotationClass, values.length);	
			} else {
				constractedValues = Array.newInstance(firstValue.getClass(),
						values.length);
			}
			for (int i = 0; i < values.length; i++) {
				Object currentValue = constructAttrValue(values[i].value());
				Array.set(constractedValues, i, currentValue);
			}
			return constractedValues;
		} 
		
		if (value instanceof AnnotationDesc) {
			return Utils.createNewAnnotation((AnnotationDesc) value);
		}
		return value;
	}

	// Modification to retrieve an attribute
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	/**
	 * 
	 * @param attrName .
	 * @param attrValue .
	 */
	public void addAttribute(final String attrName, final Object attrValue) {
		attributes.put(getShortName(attrName), attrValue);
	}

	@Override
	public String toString() {
		String str = "@" + name + " ";
		if (attributes != null && attributes.size() > 0) {
			str += attributes;
		} else {
			str += "{No attributes}";
		}
		return str;
	}

}
