package nf.fr.k49.seringue.test.app.inheritage;

import nf.fr.k49.seringue.annotations.Singleton;

public class F extends E {

	@Singleton
	public F() {
	}
	
	public String print() {
		return "F";
	}
}
