package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.annotations.Singleton;

public class B {

	public C c = null;

	@Singleton
	public B(final C c) {
		this.c = c;
	}

	public String sayHello() {
		return "Hello " + c.getWho() + "!";
	}

}
