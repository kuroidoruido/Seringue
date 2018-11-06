package nf.fr.k49.seringue.test.app.inheritage;

import nf.fr.k49.seringue.annotations.Singleton;

public class I extends E implements G {

	@Singleton(ignoreSuperType = { "nf.fr.k49.seringue.test.app.inheritage.E" })
	public I() {
	}

	@Override
	public String whoAreYou() {
		return "I";
	}

	@Override
	public String print() {
		return whoAreYou();
	}

}
