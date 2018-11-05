package nf.fr.k49.seringue.compiler;

public class CaseUtils {

	private CaseUtils() {
	}

	public static String lowerFirst(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
}
