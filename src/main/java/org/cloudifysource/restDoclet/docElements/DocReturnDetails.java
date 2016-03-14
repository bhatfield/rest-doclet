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

import com.sun.javadoc.Type;
import java.util.List;

/**
 * 
 * @author yael
 *
 */
public class DocReturnDetails {
	private final Type returnType;
	private String description;
	private List<DocParameter> paramsList;

	public DocReturnDetails(final Type returnType) {
		this.returnType = returnType;
	}
	public Type getReturnType() {
		return returnType;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(final String description) {
		this.description = description;
	}
	public List<DocParameter> getParamsList() { return paramsList; }
	public void setParamsList(List<DocParameter> paramsList) { this.paramsList = paramsList; }

	@Override
	public String toString() {
		String str = returnType.typeName();
		if (description != null) {
			str += ": " + description;
		}
		return str;
	}
}
