package nf.fr.k49.seringue.compiler;

import java.util.ArrayList;
import java.util.List;

public class SingletonElement<T> {

	private static final int MAX_DEPENANCY_DEPTH = 50;

	String packageName;
	String className;
	List<String> superTypes;

	List<String> constructorParams;

	List<SingletonElement<?>> dependencies;

	SingletonElement() {
		this.superTypes = new ArrayList<>();
		this.constructorParams = new ArrayList<>();
		this.dependencies = new ArrayList<>();
	}

	int complexityScore() {
		return complexityScore(0);
	}

	private int complexityScore(int level) {
		if (level > MAX_DEPENANCY_DEPTH) {
			throw new StackOverflowError(
					"Your dependency tree has a depth over " + MAX_DEPENANCY_DEPTH + " and Seringue can't manage it.");
		}
		return dependencies.stream().map(se -> se.complexityScore(level + 1)).reduce(1, Integer::sum);
	}

}
