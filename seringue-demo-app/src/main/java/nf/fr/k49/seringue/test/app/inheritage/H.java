package nf.fr.k49.seringue.test.app.inheritage;

import nf.fr.k49.seringue.annotations.Singleton;

public class H implements G {

	@Singleton
	public H() {
	}
	
	@Override
	public String whoAreYou() {
		return "H";
	}

}
