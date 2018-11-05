package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.annotations.Singleton;

public class J {

	private E e;
	private F f;
	private G g;
	private H h;
	private I i;

	@Singleton
	public J(final E e, final F f, final G g, final H h, final I i) {
		this.e = e;
		this.f = f;
		this.g = g;
		this.h = h;
		this.i = i;
	}

	public String printAll() {
		return "E(" + e + "): " + e.print() + "\n"//
				+ "F(" + f + "): " + f.print() + "\n"//
				+ "G(" + g + "): " + g.whoAreYou() + "\n"//
				+ "H(" + h + "): " + h.whoAreYou() + "\n"//
				+ "I(" + i + "): " + i.print() + "\n";
	}
}
