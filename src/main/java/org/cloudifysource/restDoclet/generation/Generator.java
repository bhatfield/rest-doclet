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
package org.cloudifysource.restDoclet.generation;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.docElements.DocAnnotation;
import org.cloudifysource.restDoclet.docElements.DocController;
import org.cloudifysource.restDoclet.docElements.DocHttpMethod;
import org.cloudifysource.restDoclet.docElements.DocMethod;
import org.cloudifysource.restDoclet.docElements.DocParameter;
import org.cloudifysource.restDoclet.docElements.DocRequestMappingAnnotation;
import org.cloudifysource.restDoclet.docElements.DocReturnDetails;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

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
	private RootDoc documentation;
	private String velocityTemplatePath;
	private String velocityTemplateFileName;
	private boolean isUserDefineTemplatePath = false;
	private String docPath;
	private String version;
	private String docCssPath;

	private static final Logger logger = Logger.getLogger(Generator.class
			.getName());

	/**
	 * 
	 * @param rootDoc
	 */
	public Generator(final RootDoc rootDoc) {
		documentation = rootDoc;
		setFlags(documentation.options());
	}

	/**
	 * @param args .
	 * @throws Exception .
	 */
	public static void main(final String[] args) throws Exception {

		com.sun.tools.javadoc.Main.execute(new String[] {
				RestDocConstants.DOCLET_FLAG, RestDoclet.class.getName(),
				RestDocConstants.SOURCE_PATH_FLAG, RestDocConstants.SOURCES_PATH, RestDocConstants.CONTROLLERS_PACKAGE,
				RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG, RestDocConstants.VELOCITY_TEMPLATE_PATH,
				RestDocConstants.DOC_DEST_PATH_FLAG, RestDocConstants.DOC_DEST_PATH, 
				RestDocConstants.DOC_CSS_PATH_FLAG, RestDocConstants.DOC_CSS_PATH,
				RestDocConstants.VERSION_FLAG, RestDocConstants.VERSION });
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
			String flagValue = options[i][contentPos];
			if (RestDocConstants.VELOCITY_TEMPLATE_PATH_FLAG.equals(flagName)) {
				velocityTemplatePath = flagValue;
				isUserDefineTemplatePath = true;
			} else if (RestDocConstants.DOC_DEST_PATH_FLAG.equals(flagName)) {
				docPath = flagValue;
			} else if (RestDocConstants.VERSION_FLAG.equals(flagName)) {
				version = flagValue;
			} else if (RestDocConstants.DOC_CSS_PATH_FLAG.equals(flagName)) {
				docCssPath = flagValue;
			}
		}

		if (velocityTemplatePath != null) {
			int fileNameIndex = velocityTemplatePath.lastIndexOf(File.separator) + 1;
			velocityTemplateFileName = velocityTemplatePath
					.substring(fileNameIndex);
			velocityTemplatePath = velocityTemplatePath.substring(0, fileNameIndex - 1);
		} else {
			velocityTemplateFileName = RestDocConstants.VELOCITY_TEMPLATE_FILE_NAME;
			velocityTemplatePath = this.getClass().getClassLoader()
					.getResource(velocityTemplateFileName).getPath();
		}

		if (docPath == null) {
			docPath = RestDocConstants.DOC_DEST_PATH;
		}
		
		if (version == null) {
			version = RestDocConstants.VERSION;
		}
		
		if (docCssPath == null) {
			docCssPath = RestDocConstants.DOC_CSS_PATH;
		}
	}

	/**
	 * 
	 * @throws Exception .
	 */
	public void run() throws Exception {

		// GENERATE DOCUMENTATIONS IN DOC CLASSES
		List<DocController> controllers = generateControllers(documentation
				.classes());

		// TRANSLATE DOC CLASSES INTO HTML DOCUMENTATION USING VELOCITY TEMPLATE
		String generatedHtml = generateHtmlDocumentation(controllers);

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
	 * @param controllers
	 * @return string that contains the documentation in HTML form.
	 * @throws Exception
	 */
	public String generateHtmlDocumentation(List<DocController> controllers)
			throws Exception {

		logger.log(Level.INFO, "Generate velocity using template: "
				+ velocityTemplatePath
				+ " ("
				+ (isUserDefineTemplatePath ? "got template path from user"
						: "default template path") + ")");

		Properties p = new Properties();
		if (isUserDefineTemplatePath) {
			p.setProperty("file.resource.loader.path", velocityTemplatePath);
		} else {
			p.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			p.setProperty("classpath.resource.loader.class",
					ClasspathResourceLoader.class.getName());
		}

		Velocity.init(p);

		VelocityContext ctx = new VelocityContext();

		ctx.put("controllers", controllers);
		ctx.put("version", version);
		ctx.put("docCssPath", docCssPath);


		Writer writer = new StringWriter();

		Template template = Velocity.getTemplate(velocityTemplateFileName);
		template.merge(ctx, writer);

		return writer.toString();

	}

	private static List<DocController> generateControllers(ClassDoc[] classes) {
		List<DocController> controllers = new LinkedList<DocController>();
		for (ClassDoc classDoc : classes) {
			DocController controller = generateController(classDoc);
			if (controller == null)
				continue;
			controllers.add(controller);
		}
		return controllers;
	}

	private static DocController generateController(ClassDoc classDoc) {
		DocController controller = new DocController(classDoc.typeName());
		List<DocAnnotation> annotations = generateAnnotations(classDoc
				.annotations());
		if (Utils.filterOutControllerClass(classDoc, annotations))
			return null;

		controller.setUri(Utils.getRequestMappingAnnotation(annotations)
				.getValue());
		controller.setMethods(generateMethods(classDoc.methods()));
		controller.setDescription(classDoc.commentText());

		if (StringUtils.isBlank(controller.getUri()))
			throw new IllegalArgumentException("controller class "
					+ controller.getName()
					+ " is missing request mapping annotation's value (uri).");
		if (controller.getMethods().isEmpty())
			throw new IllegalArgumentException("controller class "
					+ controller.getName() + " doesn't have methods.");

		return controller;
	}

	private static List<DocAnnotation> generateAnnotations(
			final AnnotationDesc[] annotations) {
		List<DocAnnotation> docAnnotations = new LinkedList<DocAnnotation>();
		for (AnnotationDesc annotationDesc : annotations) {
			docAnnotations.add(Utils.createNewAnnotation(annotationDesc));
		}
		return docAnnotations;
	}

	private static SortedMap<String, DocMethod> generateMethods(
			final MethodDoc[] methods) {
		SortedMap<String, DocMethod> docMethods = new TreeMap<String, DocMethod>();

		for (MethodDoc methodDoc : methods) {
			List<DocAnnotation> annotations = generateAnnotations(methodDoc
					.annotations());
			DocRequestMappingAnnotation requestMappingAnnotation = Utils
					.getRequestMappingAnnotation(annotations);

			if (requestMappingAnnotation == null) {
				continue;
			}

			DocHttpMethod httpMethod = generateHttpMethod(methodDoc,
					requestMappingAnnotation.getMethod(), annotations);
			String uri = requestMappingAnnotation.getValue();

			if (StringUtils.isBlank(uri)) {
				throw new IllegalArgumentException(
						"method "
								+ methodDoc.name()
								+ " is missing request mapping annotation's value (uri).");
			}
			// If method uri already exist, add the current httpMethod to the
			// existing method.
			// There can be several httpMethods (GET, POST, DELETE) for each
			// uri.
			DocMethod docMethod = docMethods.get(uri);
			if (docMethod != null) {
				docMethod.addHttpMethod(httpMethod);
			} else {
				docMethod = new DocMethod(httpMethod);
				docMethod.setUri(uri);
			}

			docMethods.put(docMethod.getUri(), docMethod);
		}
		return docMethods;
	}

	private static DocHttpMethod generateHttpMethod(MethodDoc methodDoc,
			String httpMethodName, List<DocAnnotation> annotations) {

		DocHttpMethod httpMethod = new DocHttpMethod(methodDoc.name(),
				httpMethodName);
		httpMethod.setDescription(methodDoc.commentText());
		httpMethod.setParams(generateParameters(methodDoc));
		httpMethod.setReturnDetails(generateReturnDetails(methodDoc));
		httpMethod.setJsonResponseExample(Utils
				.getJsonResponseExampleAnnotation(annotations));
		httpMethod.setJsonRequesteExample(Utils
				.getJsonRequestExampleAnnotation(annotations));
		httpMethod.setPossibleResponseStatuses(Utils
				.getPossibleResponseStatusesAnnotation(annotations));

		if (StringUtils.isBlank(httpMethod.getHttpMethodName())) {
			throw new IllegalArgumentException(
					"method "
							+ methodDoc.name()
							+ " is missing request mapping annotation's method (http method).");
		}

		return httpMethod;
	}

	private static List<DocParameter> generateParameters(MethodDoc methodDoc) {
		List<DocParameter> paramsList = new LinkedList<DocParameter>();

		for (Parameter parameter : methodDoc.parameters()) {
			DocParameter docParameter = new DocParameter(parameter.name(),
					parameter.type());
			docParameter.setAnnotations(generateAnnotations(parameter
					.annotations()));
			docParameter.setDescription(Utils.getParamTagsComments(methodDoc)
					.get(parameter.name()));

			paramsList.add(docParameter);
		}
		return paramsList;
	}

	private static DocReturnDetails generateReturnDetails(MethodDoc methodDoc) {
		DocReturnDetails returnDetails = new DocReturnDetails(
				methodDoc.returnType());
		Tag[] returnTags = methodDoc.tags("return");
		if (returnTags.length > 0) {
			returnDetails.setDescription(returnTags[0].text());
		}
		return returnDetails;
	}

}
