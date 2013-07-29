/*******************************************************************************

 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.restDoclet.exampleGenerators;

import org.cloudifysource.restDoclet.docElements.DocHttpMethod;
import org.cloudifysource.restDoclet.docElements.DocParameter;

/**
 * 
 * @author yael
 *
 */
public class DefaultRequestBodyParameterFilter implements IRequestBodyParamFilter {

	@Override
	public boolean filter(final DocHttpMethod httpMethod, final DocParameter param) {
		return param.getRequestBodyAnnotation() != null;
	}

}