package com.nttdata.enel.EnigmaInt;

import org.apache.log4j.Logger;

public class FieldsFunct {

	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	public Object[] Elab(
			String fieldValue, 
			String[] ElabVal) {

		logger.debug("+++++ Function Parameters: " + 
				" -> " + ElabVal[0]+" - "+ElabVal[1]+" - "+ElabVal[2]+" - "+ElabVal[3]);
		boolean okToProceedExtr=true;
		String  retStringElab="";
		String  retMesgElab="";
		Object[] theRetValueElab = { okToProceedExtr , retStringElab , retMesgElab };
		
		String[] theRetValueArr=null;
		String theFieldSeparator="";
		switch (ElabVal[1]) {
		
		case "split":  // split function
			theRetValueArr=null;
			theFieldSeparator=ElabVal[2];
			theRetValueArr=fieldValue.split(theFieldSeparator);
			String theValueExtract="";
			if (theRetValueArr.length>1) {
				theValueExtract=theRetValueArr[Integer.parseInt(ElabVal[3])].trim();
			} else {
				theValueExtract=theRetValueArr[0].trim();
			}
			logger.debug("+++++ Field Function "+ElabVal[1]+" the Return Value is "+theValueExtract ); //DATA
			okToProceedExtr=true;
			retStringElab=theValueExtract;
			break;

		case "cidr":  // cidr notation
			theRetValueArr=null;
			theFieldSeparator=ElabVal[2];
			theRetValueArr=fieldValue.split(theFieldSeparator);
			String theCidr="";
			if (theRetValueArr.length>1) {
				switch (theRetValueArr[Integer.parseInt(ElabVal[3])]) {
				case "32":
					theCidr="255.255.255.255";
					break;
				case "31":
					theCidr="255.255.255.254";
					break;
				case "30":
					theCidr="255.255.255.252";
					break;
				case "29":
					theCidr="255.255.255.248";
					break;
				case "28":
					theCidr="255.255.255.240";
					break;
				case "27":
					theCidr="255.255.255.224";
					break;
				case "26":
					theCidr="255.255.255.192";
					break;
				case "25":
					theCidr="255.255.255.128";
					break;
				case "24":
					theCidr="255.255.255.0";
					break;
				case "23":
					theCidr="255.255.254.0";
					break;
				case "22":
					theCidr="255.255.252.0";
					break;
				case "21":
					theCidr="255.255.248.0";
					break;
				case "20":
					theCidr="255.255.240.0";
					break;
				case "19":
					theCidr="255.255.224.0";
					break;
				case "18":
					theCidr="255.255.192.0";
					break;
				case "17":
					theCidr="255.255.128.0";
					break;
				case "16":
					theCidr="255.255.0.0";
					break;
				default:
					theCidr="";
				}
				
				if (theCidr=="") {
					okToProceedExtr=false;
					retStringElab=theCidr;
					retMesgElab="Sunnet Mask not defined for field";
				} else {
					okToProceedExtr=true;
					retStringElab=theCidr;
				}				
			} else {
				okToProceedExtr=true;
				retStringElab=theCidr;
			}
			logger.debug("+++++ Field Function "+ElabVal[1]+" the Return Value: "+theCidr );  //DATA

			break;			
		}
		
		theRetValueElab[0]=okToProceedExtr;
		theRetValueElab[1]=retStringElab;
		theRetValueElab[2]=retMesgElab;
		return theRetValueElab;
	}
	

}
