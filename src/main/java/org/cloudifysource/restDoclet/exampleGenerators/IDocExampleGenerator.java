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

/**
 * 
 * @author yael
 *
 */
public interface IDocExampleGenerator {
	
	/**
	 * Creates an example of a response or request for documentation of a certain REST method.
	 * @param clazz
	 * 		   The type of which the example will be created.
	 * 			for example: 
	 * 				the type of the return value of the documented method, 
	 * 				in case of creating an example of a response for that method. 
	 * @return An example. 
	 * @throws Exception . 
	 */
	String generateExample(Class<?> clazz) throws Exception;
}