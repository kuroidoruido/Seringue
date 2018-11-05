package nf.fr.k49.seringue.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;

@SupportedAnnotationTypes(value = { "nf.fr.k49.seringue.annotations.Singleton" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectionCompiler extends AbstractProcessor {

	private boolean exitStatus = true;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		final List<SingletonElement<?>> singletonElements = new ArrayList<>();

		extractAnnotationTargets(annotations, roundEnv, singletonElements);
		info("Extract Annotated Elements: done");
		fillSingletonElementDependencies(singletonElements);
		info("Compute Dependency Tree: done");
		final String seringueClass = createCustomSeringueInjector(singletonElements);
		if (seringueClass != null) {
			writeSeringueInFile(seringueClass);
		}
		info("Generate Custom Seringue Injector: done");

		return exitStatus;
	}

	private void extractAnnotationTargets(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv,
			final List<SingletonElement<?>> singletonElements) {
		for (TypeElement te : annotations) {
			for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
				final String teSimpleName = te.getSimpleName().toString();
				final TypeElement classElement = (TypeElement) element.getEnclosingElement();
				final PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();
				info("Treat " + classElement.getQualifiedName().toString() + " for annotation " + teSimpleName);

				final SingletonElement<?> se = new SingletonElement<>();

				final ExecutableElement execElement = (ExecutableElement) element;
				se.constructorParams = execElement.getParameters().stream()//
						.map(ve -> ve.asType().toString())//
						.collect(Collectors.toList());
				se.className = classElement.getSimpleName().toString();
				se.packageName = packageElement.getQualifiedName().toString();

				singletonElements.add(se);
			}
		}
	}

	private void fillSingletonElementDependencies(final List<SingletonElement<?>> singletonElements) {
		final List<SingletonElement<?>> input = new ArrayList<>(singletonElements.size());
		input.addAll(singletonElements);

		final Map<String, SingletonElement<?>> filled = new HashMap<>();
		while (!input.isEmpty()) {
			final Iterator<SingletonElement<?>> it = input.iterator();
			while (it.hasNext()) {
				final SingletonElement<?> se = it.next();

				if (se.constructorParams.isEmpty()) {
					filled.put(se.packageName + "." + se.className, se);
					it.remove();
				} else if (se.constructorParams.stream().allMatch(cp -> filled.containsKey(cp))) {
					se.dependencies = se.constructorParams.stream()//
							.map(cp -> filled.get(cp))//
							.collect(Collectors.toList());
					filled.put(se.packageName + "." + se.className, se);
					it.remove();
				}
			}
		}
	}

	private String createCustomSeringueInjector(List<SingletonElement<?>> singletonElements) {
		if (singletonElements.isEmpty()) {
			return null;
		}

		final StringBuilder sbSeringueClass = new StringBuilder();
		final StringBuilder sbImports = new StringBuilder();
		final StringBuilder sbFields = new StringBuilder();
		final StringBuilder sbGetMethods = new StringBuilder();

		for (SingletonElement<?> se : singletonElements) {
			// create import
			//
			// import <PackageName>.<ClassName>;
			sbImports.append("import ");
			sbImports.append(se.packageName);
			sbImports.append(".");
			sbImports.append(se.className);
			sbImports.append(";\n");

			// create static field to hold lazily created instance
			//
			// private static <ClassName> instanceOf<ClassName>;
			sbFields.append("\tprivate static ");
			sbFields.append(se.className);
			sbFields.append(" instanceOf");
			sbFields.append(se.className);
			sbFields.append(";\n");

			// create static get method with lazy init
			//
			// public synchronized static <ClassName> get<ClassName>() {
			// if(instanceOf<ClassName> == null) {
			// instanceOf<ClassName> = new <ClassName>(get<DepClassName>()...);
			// }
			// return instanceOf<ClassName>;
			// }
			sbGetMethods.append("\tpublic synchronized static ");
			sbGetMethods.append(se.className);
			sbGetMethods.append(" get");
			sbGetMethods.append(se.className);
			sbGetMethods.append("() {\n");

			sbGetMethods.append("\t\tif (instanceOf");
			sbGetMethods.append(se.className);
			sbGetMethods.append(" == null) {\n");

			sbGetMethods.append("\t\t\tinstanceOf");
			sbGetMethods.append(se.className);
			sbGetMethods.append(" = new ");
			sbGetMethods.append(se.className);
			sbGetMethods.append("(");
			sbGetMethods.append(se.dependencies.stream()//
					.map(depSe -> "get" + depSe.className + "()")//
					.collect(Collectors.joining(", ")));
			sbGetMethods.append(");\n");

			sbGetMethods.append("\t\t}\n");
			sbGetMethods.append("\t\treturn instanceOf");
			sbGetMethods.append(se.className);
			sbGetMethods.append(";\n");
			sbGetMethods.append("\t}\n\n");// get<ClassName> method end
		}

		sbSeringueClass.append("package nf.fr.k49.seringue;\n\n");
		sbSeringueClass.append(sbImports.toString());
		sbSeringueClass.append("\n");
		sbSeringueClass.append("public class Seringue {\n");
		sbSeringueClass.append(sbFields.toString());
		sbSeringueClass.append("\n");
		sbSeringueClass.append(sbGetMethods.toString());
		sbSeringueClass.append("}");// class end
		return sbSeringueClass.toString();
	}

	private void writeSeringueInFile(final String seringueClass) {
		final Filer filer = processingEnv.getFiler();

		try (final PrintWriter pw = new PrintWriter(
				filer.createSourceFile("nf.fr.k49.seringue.Seringue").openOutputStream())) {
			pw.println(seringueClass);
		} catch (IOException e) {
			err("Impossible to create class nf.fr.k49.seringue.Seringue. " + e);
		}
	}

	private void info(String msg) {
		processingEnv.getMessager().printMessage(Kind.NOTE, msg);
	}

	private void err(String msg) {
		processingEnv.getMessager().printMessage(Kind.ERROR, msg);
		exitStatus = false;
	}
}
