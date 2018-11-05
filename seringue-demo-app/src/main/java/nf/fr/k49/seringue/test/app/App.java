package nf.fr.k49.seringue.test.app;

import nf.fr.k49.seringue.Seringue;

public class App {

	public static void main(String[] args) {
		System.out.println(Seringue.getA());
		System.out.println(Seringue.getA().print());
		System.out.println(Seringue.getB());
		System.out.println(Seringue.getB().sayHello());
		System.out.println(Seringue.getC());
		System.out.println(Seringue.getC().getWho());
		System.out.println(Seringue.getD());
		System.out.println(Seringue.getD().print());
		System.out.println(Seringue.getJ().printAll());
	}
}
