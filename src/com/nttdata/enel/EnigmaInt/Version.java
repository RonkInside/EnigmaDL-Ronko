package com.nttdata.enel.EnigmaInt;

import org.apache.log4j.Logger;

public class Version {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	static String ver = "0.2.5dev";

	public String getVersion() {
		return ver;
	}

	public static void main(String[] args) {
		logger.info(ver);
	}
}
