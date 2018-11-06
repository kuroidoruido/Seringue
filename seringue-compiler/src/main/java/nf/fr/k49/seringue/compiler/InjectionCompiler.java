package nf.fr.k49.seringue.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic.Kind;

import nf.fr.k49.seringue.annotations.SeringueApp;
import nf.fr.k49.seringue.annotations.Singleton;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;

@SupportedAnnotationTypes(value = { "nf.fr.k49.seringue.annotations.Singleton",
		"nf.fr.k49.seringue.annotations.SeringueApp" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectionCompiler extends AbstractProcessor {

	private boolean exitStatus = true;
	private String seringueInjectorPackage = null;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		final List<SingletonElement<?>> singletonElements = new ArrayList<>();

		if (seringueInjectorPackage == null) {
			seringueInjectorPackage = extractSeringueAppAnnotation(annotations, roundEnv);
		}
		if (exitStatus) {
			extractAnnotationTargets(annotations, roundEnv, singletonElements);
			if (!singletonElements.isEmpty()) {
				info("Extract Annotated Elements: done");
				fillSingletonElementDependencies(singletonElements);
				info("Compute Dependency Tree: done");
				final String seringueClass = createCustomSeringueInjector(singletonElements);
				if (seringueClass != null) {
					writeSeringueInFile(seringueClass);
				}
				info("Generate Custom Seringue Injector: done");
			}
		}
		return exitStatus;
	}

	private String extractSeringueAppAnnotation(final Set<? extends TypeElement> annotations,
			final RoundEnvironment roundEnv) {
		final Iterator<? extends TypeElement> it = annotations.iterator();
		while (it.hasNext()) {
			final TypeElement ano = it.next();
			if (SeringueApp.class.getSimpleName().equals(ano.getSimpleName().toString())) {
				final Set<? extends Element> classes = roundEnv.getElementsAnnotatedWith(ano);
				if (classes.size() != 1) {
					break;
				}

				it.remove();
				final TypeElement mainClassElement = (TypeElement) classes.toArray()[0];
				final PackageElement mainClassPackageElement = (PackageElement) mainClassElement.getEnclosingElement();
				return mainClassPackageElement.getQualifiedName().toString();
			}
		}
		err("You must use @SeringueApp annotation on your main class.");
		return null;
	}

	private void extractAnnotationTargets(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv,
			final List<SingletonElement<?>> singletonElements) {
		debug("extractAnnotationTargets() started");
		for (TypeElement te : annotations) {
			for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
				final String teSimpleName = te.getSimpleName().toString();
				final TypeElement classElement = (TypeElement) element.getEnclosingElement();
				final PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();
				info("Treat " + classElement.getQualifiedName().toString() + " for annotation " + teSimpleName);
				final List<String> ignoredSuperType = Arrays
						.asList(element.getAnnotation(Singleton.class).ignoreSuperType());

				final SingletonElement<?> se = new SingletonElement<>();

				final ExecutableElement execElement = (ExecutableElement) element;
				se.constructorParams = execElement.getParameters().stream()//
						.map(ve -> ve.asType().toString())//
						.collect(Collectors.toList());
				se.className = classElement.getSimpleName().toString();
				se.packageName = packageElement.getQualifiedName().toString();

				// add as super type all super classes
				for (TypeElement cur = getSuperType(classElement); //
						!"java.lang.Object".equals(cur.getQualifiedName().toString()); //
						cur = getSuperType(cur)) {
					final String curQualifiedName = cur.getQualifiedName().toString();
					final boolean ignored = ignoredSuperType.contains(curQualifiedName);
					debug("Found superclass " + cur.getQualifiedName() + " for " + se.packageName + "." + se.className//
							+ (ignored ? " but it is ignored" : ""));
					if (!ignored) {
						se.superTypes.add(curQualifiedName);
					}
				}
				// add as super type all interfaces
				se.superTypes.addAll(classElement.getInterfaces().stream()//
						.map(i -> (TypeElement) ((DeclaredType) i).asElement())//
						.map(e -> e.getQualifiedName().toString())//
						.filter(e -> !ignoredSuperType.contains(e))//
						.collect(Collectors.toList())//
				);

				singletonElements.add(se);
			}
		}
		debug("extractAnnotationTargets() end");
	}

	private static TypeElement getSuperType(TypeElement te) {
		return (TypeElement) ((DeclaredType) te.getSuperclass()).asElement();
	}

	private void fillSingletonElementDependencies(final List<SingletonElement<?>> singletonElements) {
		debug("fillSingletonElementDependencies() start");
		final List<SingletonElement<?>> input = new ArrayList<>(singletonElements.size());
		input.addAll(singletonElements);

		final Map<String, SingletonElement<?>> filled = new HashMap<>();
		int loopCounter = singletonElements.size();
		while (!input.isEmpty()) {
			if (loopCounter < 0) {
				err("Impossible to fill these classes: " + input + "\nwith these singletons: " + filled.keySet());
				break;
			}
			final Iterator<SingletonElement<?>> it = input.iterator();
			while (it.hasNext()) {
				final SingletonElement<?> se = it.next();
				debug("Filling " + se.packageName + "." + se.className);

				if (se.constructorParams.isEmpty()) {
					debug("   marked as filled (no dependencies)");
					filled.put(se.packageName + "." + se.className, se);
					declareSingletonForEachSuperTypes(filled, se.superTypes, se);
					it.remove();
				} else if (se.constructorParams.stream().allMatch(cp -> filled.containsKey(cp))) {
					debug("   cannot be filled yet (all dependencies are filled)");
					se.dependencies = se.constructorParams.stream()//
							.map(cp -> filled.get(cp))//
							.collect(Collectors.toList());
					filled.put(se.packageName + "." + se.className, se);
					declareSingletonForEachSuperTypes(filled, se.superTypes, se);
					it.remove();
				} else {
					debug("   cannot be filled yet");
				}
			}
			loopCounter--;
		}
		debug("fillSingletonElementDependencies() end");
	}

	private void declareSingletonForEachSuperTypes(final Map<String, SingletonElement<?>> map, final List<String> keys,
			final SingletonElement<?> value) {
		SingletonElement<?> old = null;
		for (String k : keys) {
			if ((old = map.put(k, value)) != null) {
				warn("Cannot be sure about which implementation of " + k + " will used between " //
						+ old.packageName + "." + old.className + " and " + value.packageName + "." + value.className);
			}
		}
	}

	private String createCustomSeringueInjector(List<SingletonElement<?>> singletonElements) {
		debug("createCustomSeringueInjector() start");
		if (singletonElements.isEmpty()) {
			return null;
		}

		final StringBuilder sbSeringueClass = new StringBuilder();
		final StringBuilder sbImports = new StringBuilder();
		final StringBuilder sbFields = new StringBuilder();
		final StringBuilder sbGetMethods = new StringBuilder();

		for (SingletonElement<?> se : singletonElements) {
			debug("Create Seringue code for " + se.packageName + "." + se.className);
			final String lowerFirstClassName = CaseUtils.lowerFirst(se.className);
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
			// private static <ClassName> <className>;
			sbFields.append("\tprivate static ");
			sbFields.append(se.className);
			sbFields.append(" ");
			sbFields.append(lowerFirstClassName);
			sbFields.append(";\n");

			// create static get method with lazy init
			//
			// public synchronized static <ClassName> get<ClassName>() {
			// if(<className> == null) {
			// <className> = new <ClassName>(get<DepClassName>()...);
			// }
			// return instanceOf<ClassName>;
			// }
			sbGetMethods.append("\tpublic synchronized static ");
			sbGetMethods.append(se.className);
			sbGetMethods.append(" get");
			sbGetMethods.append(se.className);
			sbGetMethods.append("() {\n");

			sbGetMethods.append("\t\tif (");
			sbGetMethods.append(lowerFirstClassName);
			sbGetMethods.append(" == null) {\n");

			sbGetMethods.append("\t\t\t");
			sbGetMethods.append(lowerFirstClassName);
			sbGetMethods.append(" = new ");
			sbGetMethods.append(se.className);
			sbGetMethods.append("(");
			sbGetMethods.append(se.dependencies.stream()//
					.map(depSe -> "get" + depSe.className + "()")//
					.collect(Collectors.joining(", ")));
			sbGetMethods.append(");\n");

			sbGetMethods.append("\t\t}\n");
			sbGetMethods.append("\t\treturn ");
			sbGetMethods.append(lowerFirstClassName);
			sbGetMethods.append(";\n");
			sbGetMethods.append("\t}\n\n");// get<ClassName> method end
		}

		sbSeringueClass.append("package ");
		sbSeringueClass.append(this.seringueInjectorPackage);
		sbSeringueClass.append(";\n\n");
		sbSeringueClass.append(sbImports.toString());
		sbSeringueClass.append("\n");
		sbSeringueClass.append("public class Seringue {\n");
		sbSeringueClass.append(sbFields.toString());
		sbSeringueClass.append("\n");
		sbSeringueClass.append(sbGetMethods.toString());
		sbSeringueClass.append("}");// class end
		debug("createCustomSeringueInjector() end");
		return sbSeringueClass.toString();
	}

	private void writeSeringueInFile(final String seringueClass) {
		debug("writeSeringueInFile() start");
		final Filer filer = processingEnv.getFiler();
		final String seringueQualifiedName = this.seringueInjectorPackage + ".Seringue";

		try (final PrintWriter pw = new PrintWriter(filer.createSourceFile(seringueQualifiedName).openOutputStream())) {
			pw.println(seringueClass);
			info("Seringue injector class was created with name: " + seringueQualifiedName);
		} catch (IOException e) {
			err("Impossible to create class nf.fr.k49.seringue.Seringue. " + e);
		}
		debug("writeSeringueInFile() end");
	}

	private void debug(String msg) {
		// System.out.println("[DEBUG] " + msg);
	}

	private void info(String msg) {
		processingEnv.getMessager().printMessage(Kind.NOTE, msg);
	}

	private void warn(String msg) {
		processingEnv.getMessager().printMessage(Kind.WARNING, msg);
	}

	private void err(String msg) {
		processingEnv.getMessager().printMessage(Kind.ERROR, msg);
		exitStatus = false;
	}
}
