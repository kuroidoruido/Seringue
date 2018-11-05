package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.annotations.Singleton;

public class A {

	public B b;

	@Singleton
	public A(final B b) {
		this.b = b;
	}

	public String print() {
		return "A: " + b.sayHello();
	}
	
}
