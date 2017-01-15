package io.nlopez.androidannotations.viewbyid;

import com.intellij.codeInsight.actions.ReformatAndOptimizeImportsProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mrm on 08/01/14.
 */
public class ViewByIdWriter extends WriteCommandAction.Simple {

	protected PsiFile mFile;
	protected Project mProject;
	protected PsiClass mClass;
	protected ArrayList<Element> mElements;
	protected PsiElementFactory mFactory;
	protected String mLayoutFileName;
	protected String mFieldNamePrefix;
	protected Map<String, String> mappings;

	public ViewByIdWriter(PsiFile file, PsiClass clazz, String command, ArrayList<Element> elements, String layoutFileName, String fieldNamePrefix) {
		super(clazz.getProject(), command);

		mFile = file;
		mProject = clazz.getProject();
		mClass = clazz;
		mElements = elements;
		mFactory = JavaPsiFacade.getElementFactory(mProject);
		mLayoutFileName = layoutFileName;
		mFieldNamePrefix = fieldNamePrefix;
		mappings = createMappings();
	}

	private Map<String, String> createMappings() {
		HashMap<String, String> mapping = new HashMap<String, String>();
		mapping.put("WebView", "android.webkit.WebView");
		mapping.put("View", "android.view.View");
		mapping.put("ViewStub", "android.view.ViewStub");
		mapping.put("TextureView", "android.view.TextureView");
		mapping.put("fragment", "android.app.Fragment");
		return Collections.synchronizedMap(mapping);
	}

	@Override
	protected void run() throws Throwable {
		/*
		PsiClass injectViewClass = JavaPsiFacade.getInstance(mProject).findClass("org.androidannotations.annotation.ViewById", new EverythingGlobalScope(mProject));
		if (injectViewClass == null) {
			return; // Butterknife library is not available for project
		}
        */
		generateFields();

		// reformat class
		JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
		styleManager.optimizeImports(mFile);
		styleManager.shortenClassReferences(mClass);

		new ReformatAndOptimizeImportsProcessor(mProject, mClass.getContainingFile(), false).runWithoutProgress();
	}

	protected void generateFields() {
		// add injections into main class
		for (Element element : mElements) {
			if (!element.used) {
				continue;
			}

			StringBuilder injection = new StringBuilder();
			if ("fragment".equals(element.name) || "android.support.v4.app.Fragment".equals(element.name)) {
				injection.append("@org.androidannotations.annotations.FragmentById"); // annotation
			} else {
				injection.append("@org.androidannotations.annotations.ViewById"); // annotation
			}
			
			injection.append("(");
			injection.append(element.getFullID());
			injection.append(")");
	
			injection.append(" ");
			if (element.nameFull != null && element.nameFull.length() > 0) { // custom package+class
				injection.append(element.nameFull);
			} else if (mappings.containsKey(element.name)) { // listed class
				injection.append(mappings.get(element.name));
			} else { // android.widget
				injection.append("android.widget.");
				injection.append(element.name);
			}
			injection.append(" ");
			injection.append(element.fieldName);
			injection.append(";");
			injection.append("\n");

			mClass.add(mFactory.createFieldFromText(injection.toString(), mClass));
		}
	}
}
