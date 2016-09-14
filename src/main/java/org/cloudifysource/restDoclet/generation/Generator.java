/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.restDoclet.generation;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.javadoc.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.docElements.*;
import org.cloudifysource.restDoclet.exampleGenerators.DefaultRequestBodyParameterFilter;
import org.cloudifysource.restDoclet.exampleGenerators.DocDefaultExampleGenerator;
import org.cloudifysource.restDoclet.exampleGenerators.IDocExampleGenerator;
import org.cloudifysource.restDoclet.exampleGenerators.IRequestBodyParamFilter;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Generates REST API documentation in an HTML form. <br />
 * Uses velocity template to generate an HTML file that contains the
 * documentation.
 * <ul>
 * <li>To specify your sources change the values of
 * {@link RestDocConstants#SOURCE_PATH} and
 * {@link RestDocConstants#CONTROLLERS_PACKAGE}.</li>
 * <li>To specify different template path change the value
 * {@link RestDocConstants#VELOCITY_TEMPLATE_PATH}.</li>
 * <li>To specify the destination path of the result HTML change the value
 * {@link RestDocConstants#DOC_DEST_PATH}.</li>
 * </ul>
 * In default the Generator uses the velocity template
 * {@link RestDocConstants#VELOCITY_TEMPLATE_PATH} and writes the result to
 * {@link RestDocConstants#DOC_DEST_PATH}.
 *
 * @author yael
 *
 */
public class Generator {
	private static final Logger logger = Logger.getLogger(Generator.class.getName());
	private static final String REQUEST_HAS_NO_BODY_MSG = "request has no body";
	private static final String RESPONSE_HAS_NO_BODY_MSG = "response has no body";
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static RootDoc documentation;
	private String velocityTemplatePath;
	private String velocityTemplateFileName;
	private boolean isUserDefineTemplatePath = false;
	private String docPath;
	private String version;
	private String docCssPath;
	private static String requestExampleGeneratorName;
	private static String responseExampleGeneratorName;
	private static IDocExampleGenerator requestExampleGenerator;
	private static IDocExampleGenerator responseExampleGenerator;
	private static String requestBodyParamFilterName;
	private static IRequestBodyParamFilter requestBodyParamFilter;
	private static Set<ClassDoc> includeEnums = new HashSet<>();
	private static List<String> paramStack = new ArrayList<>();

	/**
	 *
	 * @param rootDoc
	 */
	public Generator(final RootDoc rootDoc) {
		documentation = rootDoc;
		setFlags(documentation.options());
	}

	/**
	 *
	 * @param args .
	 * <p>This class uses the annotationType() method of class DocAnnotation,
	 * so if there is an annotation in the source with its class not in the class path,
	 * a ClassCastException will be thrown.
	 * <br>For example, in order to use the PreAuthorize annotation,
	 * the spring-security-core JAR needs to be added to the class path.
	 * <br><a href="http://stackoverflow.com/questions/5314738/javadoc-annotations-from-third-party-libraries">
	 * related question in stackoverflow</a>
	 */
	public static void main(final String[] args) {

		/**
		 * This class uses the annotationType() method of class DocAnnotation,
		 * so if there is an annotation in the source which its class is not in the class path,
		 * a ClassCastException will be thrown.
		 * For example, to use the PreAuthorize annotation,
		 * the spring-security-core JAR need to be added to the class path.
		 * See <a href="http://stackoverflow.com/questions/5314738/javadoc-annotations-from-third-party-libraries">
		 * related question in stackoverflow</a>
		 **/
		com.sun.tools.javadoc.Main.execute(new String[]{
				RestDocConstants.DOCLET_FLAG, RestDoclet.class.getName(),
				RestDocConstants.SOURCE_PATH_FLAG, RestDocConstants.SOURCES_PATH, RestDocConstants.CONTROLLERS_PACKAGE,
				RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG, RestDocConstants.VELOCITY_TEMPLATE_PATH,
				RestDocConstants.DOC_DEST_PATH_FLAG, RestDocConstants.DOC_DEST_PATH,
				RestDocConstants.DOC_CSS_PATH_FLAG, RestDocConstants.DOC_CSS_PATH,
				RestDocConstants.VERSION_FLAG, RestDocConstants.VERSION
		});
	}

	/**
	 *
	 * @param options
	 */
	private void setFlags(final String[][] options) {
		int flagPos = 0;
		int contentPos = 1;
		for (int i = 0; i < options.length; i++) {
			String flagName = options[i][flagPos];
			String flagValue = null;
			if (options[i].length > 1) {
				flagValue = options[i][contentPos];
			}
			if (RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG.equals(flagName)) {
				velocityTemplatePath = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.DOC_DEST_PATH_FLAG.equals(flagName)) {
				docPath = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.VERSION_FLAG.equals(flagName)) {
				version = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.DOC_CSS_PATH_FLAG.equals(flagName)) {
				docCssPath = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.REQUEST_EXAMPLE_GENERATOR_CLASS_FLAG.equals(flagName)) {
				requestExampleGeneratorName = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.RESPONSE_EXAMPLE_GENERATOR_CLASS_FLAG.equals(flagName)) {
				responseExampleGeneratorName = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			} else if (RestDocConstants.REQUEST_BODY_PARAM_FILTER_CLASS_FLAG.equals(flagName)) {
				requestBodyParamFilterName = flagValue;
				logger.log(Level.INFO, "Updating flag " + flagName + " value = " + flagValue);
			}
		}

		if (!StringUtils.isBlank(velocityTemplatePath)) {
			isUserDefineTemplatePath = true;
			int fileNameIndex = velocityTemplatePath.lastIndexOf(File.separator) + 1;
			velocityTemplateFileName = velocityTemplatePath.substring(fileNameIndex);
			velocityTemplatePath = velocityTemplatePath.substring(0, fileNameIndex - 1);
		} else {
			velocityTemplateFileName = RestDocConstants.VELOCITY_TEMPLATE_FILE_NAME;
			velocityTemplatePath = this.getClass().getClassLoader()
					.getResource(velocityTemplateFileName).getPath();
		}

		if (StringUtils.isBlank(docPath)) {
			docPath = RestDocConstants.DOC_DEST_PATH;
		}

		if (StringUtils.isBlank(version)) {
			version = RestDocConstants.VERSION;
		}

		if (StringUtils.isBlank(docCssPath)) {
			docCssPath = RestDocConstants.DOC_CSS_PATH;
		}

		initRequestExampleGenerator(requestExampleGeneratorName);
		logger.log(Level.INFO, "Updating request example generator class to "
				+ requestExampleGenerator.getClass().getName());
		initResponseExampleGenerator(responseExampleGeneratorName);
		logger.log(Level.INFO, "Updating response example generator class to "
				+ responseExampleGenerator.getClass().getName());

		initRequestBodyParamFilter();
		logger.log(Level.INFO, "Updating request body parameter filter class to "
				+ requestBodyParamFilter.getClass().getName());
	}


	private void initRequestBodyParamFilter() {
		if (StringUtils.isBlank(requestBodyParamFilterName)) {
			requestBodyParamFilter = new DefaultRequestBodyParameterFilter();
		} else {
			try {
				Class<?> clazz = Class.forName(requestBodyParamFilterName);
				requestBodyParamFilter = (IRequestBodyParamFilter) clazz.newInstance();
			} catch (Exception e) {
				logger.log(Level.WARNING,
						"Cought " + e.getClass().getName()
								+ " when tried to load and instantiate class "
								+ requestBodyParamFilterName
								+ ". Using a default filter class instead.");
				requestBodyParamFilter = new DefaultRequestBodyParameterFilter();
			}
		}
	}

	private void initRequestExampleGenerator(final String exampleGeneratorName) {
		IDocExampleGenerator exampleGeneratorClass =
				getExampleGeneratorClass(
						IDocExampleGenerator.class,
						exampleGeneratorName,
						"request");
		if (exampleGeneratorClass == null) {
			requestExampleGenerator = new DocDefaultExampleGenerator();
		} else {
			requestExampleGenerator = exampleGeneratorClass;
		}
	}

	private void initResponseExampleGenerator(final String exampleGeneratorName) {
		IDocExampleGenerator exampleGeneratorClass =
				getExampleGeneratorClass(
						IDocExampleGenerator.class,
						exampleGeneratorName,
						"response");
		if (exampleGeneratorClass == null) {
			responseExampleGenerator = new DocDefaultExampleGenerator();
		} else {
			responseExampleGenerator = exampleGeneratorClass;
		}
	}

	private <T> T getExampleGeneratorClass(
			final Class<T> expectedInterface,
			final String exampleGeneratorName,
			final String exampleType) {
		if (StringUtils.isBlank(exampleGeneratorName)) {
			logger.log(Level.INFO,
					"No custom example generator given, using a default "
							+ exampleType + " example generator instead.");
			return null;
		}

		Class<?> reqExGenClass;
		try {
			reqExGenClass = Class.forName(exampleGeneratorName);
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING,
					"Cought ClassNotFoundException when tried to load the " + exampleType
							+ " example generator class - "
							+ exampleGeneratorName
							+ ". Using a default generator instead.");
			return null;
		}
		if (!expectedInterface.isAssignableFrom(reqExGenClass)) {
			logger.log(Level.WARNING,
					"The given " + exampleType
							+ " example generator class [" + exampleGeneratorName
							+ "] does not implement " + expectedInterface.getName()
							+ ". Using a default generator instead.");
			return null;
		}

		try {
			return expectedInterface.cast(reqExGenClass.newInstance());
		} catch (Exception e) {
			logger.log(Level.WARNING,
					"Cought exception - " + e.getClass().getName()
							+ " when tried to instantiate the " + exampleType
							+ " example generator class [ " + exampleGeneratorName
							+ "]. Using a default generator instead.");
			return null;
		}
	}

	/**
	 *
	 * @throws Exception .
	 */
	public void run() throws Exception {

		// GENERATE DOCUMENTATIONS IN DOC CLASSES
		ClassDoc[] classes = documentation.classes();
		List<DocController> controllers = generateControllers(classes);
		logger.log(Level.INFO, "Generated " + controllers.size()
				+ " controlles, creating HTML documentation using velocity template.");

		// TRANSLATE DOC CLASSES INTO HTML DOCUMENTATION USING VELOCITY TEMPLATE
		String generatedHtml = generateHtmlDocumentation(controllers, includeEnums);

		// WRITE GENERATED HTML TO A FILE
		FileWriter velocityfileWriter = null;
		try {
			File file = new File(docPath);
			File parentFile = file.getParentFile();
			if (parentFile != null) {
				if (parentFile.mkdirs()) {
					logger.log(
							Level.FINEST,
							"The directory "
									+ parentFile.getAbsolutePath()
									+ " was created, along with all necessary parent directories.");
				}
			}
			logger.log(Level.INFO,
					"Write generated velocity to " + file.getAbsolutePath());
			velocityfileWriter = new FileWriter(file);
			velocityfileWriter.write(generatedHtml);
		} finally {
			if (velocityfileWriter != null) {
				velocityfileWriter.close();
			}
		}
	}

	/**
	 * Creates the REST API documentation in HTML form, using the controllers'
	 * data and the velocity template.
	 *
	 * @param controllers .
	 * @return string that contains the documentation in HTML form.
	 * @throws Exception .
	 */
	public String generateHtmlDocumentation(final List<DocController> controllers, final Set dataStructs)
			throws Exception {

		logger.log(Level.INFO, "Generate velocity using template: "
				+ velocityTemplatePath
				+ (isUserDefineTemplatePath ? File.separator
				+ velocityTemplateFileName + " (got template path from user)"
				: "(default template path)"));

		Properties p = new Properties();
		p.setProperty("directive.set.null.allowed", "true");
		if (isUserDefineTemplatePath) {
			p.setProperty("file.resource.loader.path", velocityTemplatePath);
		} else {
			p.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			p.setProperty("classpath.resource.loader.class",
					ClasspathResourceLoader.class.getName());
		}

		Velocity.init(p);

		VelocityContext ctx = new VelocityContext();
		List<String> enumList = new ArrayList<>();

		Iterator<ClassDoc> i = dataStructs.iterator();
		List<ClassDoc> l = new ArrayList<>();
		while (i.hasNext()) {
			ClassDoc doc = i.next();
			l.add(doc);
			enumList.add(doc.simpleTypeName());
		}

		ctx.put("controllers", controllers);
		ctx.put("version", version);
		ctx.put("docCssPath", docCssPath);
		ctx.put("dataStructs", l);
		ctx.put("enumList", enumList);

		Writer writer = new StringWriter();

		Template template = Velocity.getTemplate(velocityTemplateFileName);
		template.merge(ctx, writer);

		return writer.toString();

	}

	private static List<DocController> generateControllers(final ClassDoc[] classes)
			throws Exception {
		List<DocController> controllersList = new LinkedList<DocController>();
		for (ClassDoc classDoc : classes) {
			List<DocController> controllers = generateControllers(classDoc);
			if (controllers == null || controllers.isEmpty()) {
				continue;
			}
			controllersList.addAll(controllers);
		}
		return controllersList;
	}

	private static List<DocController> generateControllers(final ClassDoc classDoc)
			throws Exception {
		List<DocController> controllers = new LinkedList<DocController>();
		List<DocAnnotation> annotations = generateAnnotations(classDoc.annotations());

		if (Utils.filterOutControllerClass(classDoc, annotations)) {
			return null;
		}

		String controllerClassName = classDoc.typeName();
		DocRequestMappingAnnotation requestMappingAnnotation = Utils.getRequestMappingAnnotation(annotations);

		String[] uriArray = new String[]{};

		if (requestMappingAnnotation == null) {
			MethodDoc[] methods = classDoc.methods();
			for (MethodDoc method : methods) {
				AnnotationDesc[] annotes = method.annotations();
				for (AnnotationDesc annote : annotes) {
					String annoteString = annote.toString();
					if (annoteString.contains("value") && annoteString.contains("method")) {
						uriArray = new String[]{""};
						break;
					}
				}
			}
			if (uriArray.length <= 0) {

				logger.log(Level.WARNING,
						"controller class " + controllerClassName
								+ " is missing request mapping annotation");
				return null;
			}
		} else {
			uriArray = requestMappingAnnotation.getValue();
		}

		if (uriArray == null || uriArray.length == 0) {
			throw new IllegalArgumentException("controller class "
					+ controllerClassName
					+ " is missing request mapping annotation's value (uri).");
		}
		for (String uri : uriArray) {
			DocController controller = new DocController(controllerClassName);
			Map<String, Type> parameterizedTypes = new HashMap<>();
			SortedMap<String, DocMethod> generatedMethods = generateMethods(classDoc.methods(), parameterizedTypes);
			Type superCls = classDoc.superclassType();
			while (superCls != null) {
				ClassDoc supCls = documentation.classNamed(superCls.qualifiedTypeName());
				if (supCls == null || isJavaType(superCls.qualifiedTypeName())) {
					break;
				}

				// Check for parameterized type
				// If parameterized, try to create a mapping between the type variable and the type
				ParameterizedType pt = superCls.asParameterizedType();
				if (pt != null) {
					Type[] params = pt.typeArguments();
					int index = 0;
					for (TypeVariable var : classDoc.superclass().typeParameters()) {
						parameterizedTypes.put(var.toString(), documentation.classNamed(params[index].qualifiedTypeName()));
						index++;
					}
				}

				MethodDoc[] parentMethods = superCls.asClassDoc().methods();
				generatedMethods.putAll(generateMethods(parentMethods, parameterizedTypes));

				superCls = supCls.superclassType();
			}

			if (generatedMethods.isEmpty()) {
				logger.log(Level.WARNING, "Could not find methods in controller: "
						+ controller.getName() + " or its parent class(es).");
				continue;
			}

			controller.setMethods(generatedMethods);
			if (uri.endsWith("/*")) {
				uri = uri.substring(0, uri.length() - 2);
			}
			controller.setUri(uri);
			controller.setDescription(classDoc.commentText());

			controllers.add(controller);
		}
		return controllers;
	}

	private static List<DocAnnotation> generateAnnotations(
			final AnnotationDesc[] annotations) {
		List<DocAnnotation> docAnnotations = new LinkedList<DocAnnotation>();
		for (AnnotationDesc annotationDesc : annotations) {
			// Skip non-spring annotations (only Valid for now)
			if (annotationDesc.annotationType().name().equals("Valid")) {
				continue;
			}
			docAnnotations.add(Utils.createNewAnnotation(annotationDesc));
		}
		return docAnnotations;
	}

	private static SortedMap<String, DocMethod> generateMethods(
			final MethodDoc[] methods, Map<String, Type> parameterizedTypes)
			throws Exception {
		SortedMap<String, DocMethod> docMethods = new TreeMap<String, DocMethod>();

		for (MethodDoc methodDoc : methods) {
			List<DocAnnotation> annotations = generateAnnotations(methodDoc.annotations());

			// Does not handle methods without a RequestMapping annotation.
			if (Utils.filterOutMethod(methodDoc, annotations)) {
				continue;
			}
			// get all HTTP methods
			DocRequestMappingAnnotation requestMappingAnnotation = Utils
					.getRequestMappingAnnotation(annotations);
			String[] methodArray = requestMappingAnnotation.getMethod();

			if (methodArray == null || methodArray.length <= 0) {
				// If we have a valid Request annotation, just assume it's a GET method
				methodArray = new String[]{"GET"};
				//continue;
			}

			List<DocHttpMethod> docHttpMethodArray = new ArrayList<>();
			for (int i = 0; i < methodArray.length; i++) {
				if (methodDoc.returnType().typeName().equals("ModelAndView")) {
					continue;
				}
				// Hack-ily handle parameterized-parameterized return type
				ParameterizedType pt = methodDoc.returnType().asParameterizedType();
				LinkedList<ParameterizedType> ptList = new LinkedList<>();
				Set<Type> params = new HashSet<>();
				if (pt != null) {
					ptList.add(pt);
				}
				// Find all type variables
				while (!ptList.isEmpty()) {
					pt = ptList.pop();
					Type[] args = pt.typeArguments();
					for (Type arg : args) {
						pt = arg.asParameterizedType();
						if (pt == null) {
							if (isJavaType(arg.qualifiedTypeName()) || documentation.classNamed(arg.qualifiedTypeName()) != null) {
								continue;
							}
							params.add(arg);
						} else {
							ptList.add(pt);
						}
					}
				}
				// Map type variable to type
				for (Type param : params) {
					String key = param.simpleTypeName();
					Type value = parameterizedTypes.get(key);
					parameterizedTypes.put(key, value);
					if (value == null) {
						value = documentation.classNamed(param.qualifiedTypeName());
						parameterizedTypes.put(key, value);
					}
				}

				docHttpMethodArray.add(generateHttpMethod(methodDoc,
						methodArray[i], annotations, parameterizedTypes));
			}
			// get all URIs
			String[] uriArray = requestMappingAnnotation.getValue();
			if (uriArray == null || uriArray.length == 0) {
				uriArray = new String[1];
				uriArray[0] = "";
			}
			if (!docHttpMethodArray.isEmpty()) {
				for (String uri : uriArray) {
					if (uri.endsWith("/*")) {
						uri = uri.substring(0, uri.length() - 2);
					}
					DocMethod docMethod = docMethods.get(uri);

					// If method with that uri already exist,
					// add the current httpMethod to the existing method.
					// There can be several httpMethods (GET, POST, DELETE) for each
					// uri.
					DocHttpMethod[] dhm = new DocHttpMethod[docHttpMethodArray.size()];
					docHttpMethodArray.toArray(dhm);
					if (docMethod != null) {
						docMethod.addHttpMethods(dhm);
					} else {
						docMethod = new DocMethod(dhm);
						docMethod.setUri(uri);
					}
					docMethods.put(uri, docMethod);
				}
			}
		}
		return docMethods;
	}

	private static DocHttpMethod generateHttpMethod(final MethodDoc methodDoc,
													final String httpMethodName,
													final List<DocAnnotation> annotations,
													Map<String,
													Type> parameterizedTypes)
			throws Exception {

		DocHttpMethod httpMethod = new DocHttpMethod(methodDoc.name(),
				httpMethodName);
		httpMethod.setDescription(methodDoc.commentText());
		httpMethod.setParams(generateParameters(methodDoc, parameterizedTypes));
		httpMethod.setReturnDetails(generateReturnDetails(methodDoc, parameterizedTypes));
		generateExamples(httpMethod, annotations, parameterizedTypes);
		httpMethod.setPossibleResponseStatuses(Utils
				.getPossibleResponseStatusesAnnotation(annotations));

		if (StringUtils.isBlank(httpMethod.getHttpMethodName())) {
			throw new IllegalArgumentException("method " + methodDoc.name()
					+ " is missing request mapping annotation's method (http method).");
		}

		return httpMethod;
	}

	private static void generateExamples(final DocHttpMethod httpMethod,
										 final List<DocAnnotation> annotations, Map<String, Type> parameterizedTypes)
			throws Exception {
		DocJsonResponseExample jsonResponseExampleAnnotation = Utils.getJsonResponseExampleAnnotation(annotations);
		DocJsonRequestExample jsonRequestExampleAnnotation = Utils.getJsonRequestExampleAnnotation(annotations);
		String requestExample;
		if (jsonRequestExampleAnnotation != null) {
			httpMethod.setJsonRequesteExample(jsonRequestExampleAnnotation);
			requestExample = jsonRequestExampleAnnotation.generateJsonRequestBody();
		} else {
			requestExample = generateRequestExmple(httpMethod, parameterizedTypes);
		}
		httpMethod.setRequestExample(requestExample);

		String responseExample;
		if (jsonResponseExampleAnnotation != null) {
			httpMethod.setJsonResponseExample(jsonResponseExampleAnnotation);
			responseExample = jsonResponseExampleAnnotation.generateJsonResponseBody();
		} else {
			responseExample = generateResponseExample(httpMethod, parameterizedTypes);
		}
		try {
			if (httpMethod.getReturnDetails().getReturnType().simpleTypeName().equals("ResponseEntity")) {
				responseExample = RESPONSE_HAS_NO_BODY_MSG;
			}
		} catch (NullPointerException e) {
			;
		}
		httpMethod.setResponseExample(responseExample);

	}

	// Test for Java type
	private static String[] blackList = {"LocalDate", "Jwt"};

	private static boolean isJavaType(String typeName) {
		String simpleTypeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		return typeName.startsWith("java") || Arrays.asList(blackList).indexOf(simpleTypeName) >= 0;
	}
	private static boolean isJavaType(Type type) {
		return isJavaType(type.qualifiedTypeName());
	}

	private static String [] javaCollections = {"java.util.Map", "java.util.List", "java.util.Set"};
	private static boolean isJavaGenericClass (String typeName) {
		return Arrays.asList(javaCollections).indexOf(typeName) >= 0;
	}
	private static boolean isJavaGenericClass (Type type) {
		return isJavaGenericClass(type.qualifiedTypeName());
	}

	private static boolean isEnum (Type type) {
		ClassDoc doc = documentation.classNamed(type.qualifiedTypeName());
		if (doc == null) {
			return false;
		} else {
			return doc.isEnum();
		}
	}

	// Returns the fields and any additional subfields of a object
	private static FieldDoc[] getFields(String name) {
		ClassDoc cls = documentation.classNamed(name);
		if (cls == null || cls.isEnum()) {
			return new FieldDoc[0];
		}
		if (cls.superclass() == null) {
			return cls.fields();
		}
		String parentCls = cls.superclass().qualifiedTypeName();
		if (!isJavaType(parentCls)) {
			return (FieldDoc[]) ArrayUtils.addAll(cls.fields(), getFields(parentCls));
		} else {
			return cls.fields();
		}
	}

	// Returns the type of the type variable
	private static Type findParameterizedType (Type type, Map<String, Type> parameterizedTypes) {
		String typeName = type.qualifiedTypeName();
		Type value = type;
		if (parameterizedTypes == null) {
			return type;
		}
		Type t = parameterizedTypes.get(typeName);
		while (t != null) {
			ClassDoc doc = documentation.classNamed(t.qualifiedTypeName());
			if (doc != null) {
				value = t;
			}
			t = parameterizedTypes.get(t);
		}
		return value;
	}


	private static Map<String, Object> handleGenericClass (String key, Type value, Map<String, Type> parameterizedTypes) {
		String typeName = value.qualifiedTypeName();
		Map<String, Object> mappy = new HashMap<>();
		if (typeName.equals("java.util.Map")) {
			// Handle Maps
			Type[] args = value.asParameterizedType().typeArguments();
			Map<String, Object> mapType = new HashMap<>();
			if (args.length == 2) {
				value = findParameterizedType(args[1], parameterizedTypes);
				paramStack.add(value.qualifiedTypeName());
				if (isJavaGenericClass(value)) {
					mappy = handleGenericClass(key, value, parameterizedTypes);
				} else if (isJavaType(value) || value.isPrimitive() || isEnum(value)) {
					mapType.put(args[0].simpleTypeName(), value.simpleTypeName());
					mappy.put(key, mapType);
				} else {
					mapType.put(args[0].simpleTypeName(), generateJSONBody(value, parameterizedTypes));
					mappy.put(key, mapType);
				}
			} else {
				mappy.put(key, "Object");
			}
		} else if (typeName.equals("java.util.List") || typeName.equals("java.util.Set")) {
			// Handle Collections
			Type[] args = value.asParameterizedType().typeArguments();
			if (args.length == 1) {
				value = findParameterizedType(args[0], parameterizedTypes);

				paramStack.add(value.qualifiedTypeName());

				if (isJavaGenericClass(value)) {
					mappy = handleGenericClass(key, value, parameterizedTypes);
				} else if (isJavaType(value) || value.isPrimitive() || isEnum(value)) {
					mappy.put(key, new String[]{value.simpleTypeName()});
				} else {
					Map temp = generateJSONBody(value, parameterizedTypes);
					Map[] array = new Map[]{temp};
					mappy.put(key, array);
				}
			} else {
				mappy.put(key, "List");
			}
		}
		paramStack.remove(paramStack.size() - 1);
		return mappy;
	}

	// Generates the JSON body for requests and responses
	private static Map generateJSONBody(Type type, Map<String, Type> parameterizedTypes) {
		Map<String, Object> mappy = new HashMap<>();

		for (FieldDoc field : getFields(type.qualifiedTypeName())) {
			String key = field.name();
			Type value = findParameterizedType(field.type(), parameterizedTypes);
			String typeName = value.qualifiedTypeName();
			ClassDoc doc = documentation.classNamed(typeName);

			// Check for circular references
			if (paramStack.contains(typeName) && !isJavaType(typeName)) {
				mappy.put(key, value.simpleTypeName());
				continue;
			}

			if (field.constantValueExpression() != null) {
				continue;
			}

			if (isJavaGenericClass(value)) {
				mappy.putAll(handleGenericClass(key, value, parameterizedTypes));
			} else if (isJavaType(typeName) || value.isPrimitive() || isEnum(value)) {
				// Other Java types, primitive and enumerations
				mappy.put(key, value.simpleTypeName());
			} else {
				// Recurse by fields for non-Java types
				paramStack.add(typeName);
				Map mapType = generateJSONBody(value, parameterizedTypes);
				if (mapType.size() <=0) {
					mappy.put(key, "Object");
				} else {
					mappy.put(key, mapType);
				}
				paramStack.remove(paramStack.size() - 1);
			}
		}
		return mappy;
	}

	// Generate example response
	// Handles Primitives, Maps, Lists
	private static String generateExample(Type type, Map<String, Type> parameterizedTypes) throws Exception {
		String typeName = type.qualifiedTypeName();
		if (isJavaGenericClass(type)) {
			Map mappy = handleGenericClass("key", type, parameterizedTypes);
			return new ObjectMapper().writeValueAsString(mappy.get("key"));
		} else if (type.dimension().length() > 0) {
			if (isJavaType(typeName) || type.isPrimitive()) {
				return new ObjectMapper().writeValueAsString(new String[]{type.simpleTypeName()});
			} else {
				List<Map> list = new ArrayList<>();
				paramStack.add(typeName);
				list.add(generateJSONBody(type, parameterizedTypes));
				paramStack.clear();
				return new ObjectMapper().writeValueAsString(list);
			}
		} else if (isJavaType(typeName) || type.isPrimitive()) {
			return new ObjectMapper().writeValueAsString(type.simpleTypeName());
		} else {
			paramStack.add(typeName);
			Map mappy = generateJSONBody(type, parameterizedTypes);
			paramStack.clear();
			return new ObjectMapper().writeValueAsString(mappy);
		}
	}

	private static String generateRequestExmple(final DocHttpMethod httpMethod, Map<String, Type> parameterizedTypes) {

		List<DocParameter> params = httpMethod.getParams();
		Type type = null;
		for (DocParameter docParameter : params) {
			if (requestBodyParamFilter.filter(httpMethod, docParameter)) {
				type = docParameter.getType();
				break;
			}
		}
		if (type == null) {
			return REQUEST_HAS_NO_BODY_MSG;
		}
		String generateExample = null;
		try {
			generateExample = generateExample(type, parameterizedTypes);
			generateExample = Utils.getIndentJson(generateExample);
		} catch (Exception e) {
			logger.warning("Could not generate request example for method: " + httpMethod.getMethodSignatureName()
					+ " with the request parameter type " + type.qualifiedTypeName()
					+ ". Exception was: " + e);
			generateExample = RestDocConstants.FAILED_TO_CREATE_REQUEST_EXAMPLE + "."
					+ LINE_SEPARATOR
					+ "Parameter type: " + type.qualifiedTypeName() + "."
					+ LINE_SEPARATOR
					+ "The exception caught was " + e;
		}
		return generateExample;
	}

	private static String generateResponseExample(final DocHttpMethod httpMethod, Map<String, Type> parameterizedTypes) {
		Type returnType = httpMethod.getReturnDetails().getReturnType();
		String typeName = returnType.qualifiedTypeName();

		if (typeName.equals(void.class.getName())) {
			String response = httpMethod.getReturnDetails().getDescription();
			// Workaround for void returns that return file content
			// Use JavaDoc @return despite void return to describe file content
			if (response == null) {
				return RESPONSE_HAS_NO_BODY_MSG;
			} else {
				return "";
			}
		}

		String generateExample = null;
		try {
			generateExample = generateExample(returnType, parameterizedTypes);
			generateExample = Utils.getIndentJson(generateExample);
		} catch (Exception e) {
			logger.warning("Could not generate response example for method: " + httpMethod.getMethodSignatureName()
					+ " with the return value type [" + typeName + "]. Exception was: " + e);
			generateExample = RestDocConstants.FAILED_TO_CREATE_RESPONSE_EXAMPLE
					+ LINE_SEPARATOR
					+ "Return value type: " + typeName + "."
					+ LINE_SEPARATOR
					+ "The exception caught was " + e;
		}

		return generateExample;
	}




	// Generate sub-parameters for the parameters table
	private static List<DocParameter> generateSubParameters (Type type,
															   String name,
															   List<DocAnnotation> annotations,
															   Map<String, Type> parameterizedTypes,
															   FieldDoc fieldInfo) {
		type = findParameterizedType(type, parameterizedTypes);
		String typeName = type.qualifiedTypeName();
		DocParameter docParameter = new DocParameter(name, type);
		Boolean required = false;
		ClassDoc doc = documentation.classNamed(typeName);
		List<DocParameter> paramsList = new LinkedList<DocParameter>();

		// Set annotations from FieldDoc or ClassDoc
		ProgramElementDoc ped = null;
		if (doc != null) {
			ped = doc;
			if (doc.isEnum()) {
				includeEnums.add(doc);
			}
		}
		if (fieldInfo != null) {
			ped = fieldInfo;
		}

		if (ped != null) {
			for (AnnotationDesc annotation : ped.annotations()) {
				if (annotation.annotationType().name().equals("NotNull")) {
					required = true;
				}
			}
			docParameter.setDescription(ped.commentText());
		}

		for (DocAnnotation annotation : annotations) {
			if (annotation.getName().equals(RestDocConstants.REQUEST_BODY_ANNOTATION)) {
				annotation.addAttribute(RestDocConstants.REQUEST_PARAMS_REQUIRED, required);
			}
		}

		docParameter.setAnnotations(annotations);
		paramsList.add(docParameter);

		// Check for circular references
		if (paramStack.contains(typeName) && !isJavaType(typeName)) {
			return paramsList;
		} else {
			paramStack.add(typeName);
		}

		// If Map, Set or List, recurse by element/value
		Type element = null;
		if (typeName.equals("java.util.Map")) {
			Type[] args = type.asParameterizedType().typeArguments();
			if (args.length == 2) {
				element = args[1];
				if (documentation.classNamed(element.qualifiedTypeName()) == null) {
					element = parameterizedTypes.get(element.simpleTypeName());
				}
			}
		} else if (typeName.equals("java.util.List") || typeName.equals("java.util.Set")) {
			Type[] args = type.asParameterizedType().typeArguments();
			if (args.length == 1) {
				element = args[0];
				if (documentation.classNamed(element.qualifiedTypeName()) == null) {
					element = parameterizedTypes.get(element.simpleTypeName());
				}
			}
		}

		// Recurse by element
		if (element != null && (!isJavaType(element) || isJavaGenericClass(element))) {
			List<DocParameter> list = generateSubParameters(element, name, annotations, parameterizedTypes, null);
			list.remove(0);
			paramsList.addAll(list);
		} else if (!isJavaType(typeName) ) {
			// Recurse by fields for non-Java types
			for (FieldDoc field : getFields(typeName)) {
				if (field.constantValueExpression() != null) {
					continue;
				}
				paramStack.add(typeName);
				String fieldName = field.name();
				if (name.length() > 0) {
					fieldName = name + "." + fieldName;
				}
				paramsList.addAll(generateSubParameters(field.type(), fieldName, annotations, parameterizedTypes, field));
				paramStack.remove(paramStack.size() - 1);
			}
		}
		paramStack.remove(paramStack.size() - 1);

		return paramsList;
	}

	private static List<DocParameter> generateParameters(final MethodDoc methodDoc, Map<String, Type> parameterizedTypes) {
		List<DocParameter> paramsList = new LinkedList<DocParameter>();

		for (Parameter parameter : methodDoc.parameters()) {
			String name = parameter.name();
			Type paramType = findParameterizedType(parameter.type(), parameterizedTypes);
			String paramStr = paramType.qualifiedTypeName();

			if (paramStr.contains("HttpServletResponse")) {
				continue;
			}

			DocParameter docParameter = new DocParameter(name, paramType);
			List<DocAnnotation> annotations = generateAnnotations(parameter.annotations());
			for (DocAnnotation annotation : annotations) {
				if (annotation.getName().equals(RestDocConstants.REQUEST_BODY_ANNOTATION)) {
					annotation.addAttribute(RestDocConstants.REQUEST_PARAMS_REQUIRED, true);
				}
			}
			docParameter.setAnnotations(annotations);
			Map<String, String> paramTagsComments = Utils.getParamTagsComments(methodDoc);
			String description = paramTagsComments.get(name);
			if (description == null) {
				logger.warning("Missing description of parameter " + name + " of method " + methodDoc.name());
				description = "";
			}
			docParameter.setDescription(description);
			paramsList.add(docParameter);

			if (!isJavaType(paramStr) || isJavaGenericClass(paramStr)) {
				List<DocParameter> list = generateSubParameters(paramType,
						name,
						annotations,
						parameterizedTypes,
						null);
				list.remove(0);
				paramsList.addAll(list);
			}
			paramStack.clear();
		}
		return paramsList;
	}

	private static DocReturnDetails generateReturnDetails(final MethodDoc methodDoc, Map<String, Type> parameterizedTypes) {
		Type returnType = methodDoc.returnType();
		String name = returnType.qualifiedTypeName();

		ParameterizedType pt = returnType.asParameterizedType();

		if (name.contains("HttpStatus")) {
			return new DocReturnDetails(returnType);
		}
		if (pt != null) {
			Type[] params = pt.typeArguments();
			if (name.contains("ResponseEntity")) {
				if (params.length == 1) {
					returnType = documentation.classNamed(params[0].qualifiedTypeName());
				} else {
					return new DocReturnDetails(returnType);
				}
			}
			ClassDoc doc = documentation.classNamed(name);
			if (doc != null) {

				TypeVariable[] vars = doc.typeParameters();
				int index = 0;

				for (TypeVariable var : vars) {
					Type t = params[index];
					String key = var.simpleTypeName();
					pt = doc.asParameterizedType();

					while (pt != null) {
						Type[] args = pt.typeArguments();
						doc = documentation.classNamed(args[0].qualifiedTypeName());
						if (doc != null) {
							pt = doc.asParameterizedType();
						} else {
							pt = null;
						}
					}

					parameterizedTypes.put(key, params[index]);
					if (documentation.classNamed(t.qualifiedTypeName()) != null) {
						parameterizedTypes.put(key, t);
					} else {
						parameterizedTypes.put(key, parameterizedTypes.get(t.simpleTypeName()));
					}
					index++;
				}
			}
		}

		returnType = findParameterizedType(returnType, parameterizedTypes);
		DocReturnDetails returnDetails = new DocReturnDetails(returnType);
		Tag[] returnTags = methodDoc.tags("return");
		if (returnTags.length > 0) {
			returnDetails.setDescription(returnTags[0].text());
		}
		if (!isJavaType(returnType) || isJavaGenericClass(returnType)) {
			List<DocParameter> paramsList = generateSubParameters(returnType,
					"",
					new LinkedList<DocAnnotation>(),
					parameterizedTypes,
					null);
			paramsList.remove(0);
			returnDetails.setParamsList(paramsList);
		}
		paramStack.clear();

		return returnDetails;
	}

}
