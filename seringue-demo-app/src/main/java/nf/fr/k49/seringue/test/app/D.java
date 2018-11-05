package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.annotations.Singleton;

public class D {

	public B b;
	public A a;

	@Singleton
	public D(final B b, final A a) {
		this.b = b;
		this.a = a;
	}

	public String print() {
		return "D: " + a.print();
	}
	
}
