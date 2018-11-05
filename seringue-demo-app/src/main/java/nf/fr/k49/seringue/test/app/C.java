package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.annotations.Singleton;

public class C {

	private String who = "World";

	@Singleton
	public C() {
	}

	public String getWho() {
		return who;
	}

}
