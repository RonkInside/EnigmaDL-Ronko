package com.nttdata.enel.EnigmaInt;

import java.io.FileInputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class ValidateInputFile {


	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");
	static Properties         fldConfVProps       = new Properties();	
	static String             fieldConfVProps     = System.getenv("FIELDCONF");


	static String             currentStepV;
	static boolean            okToProceedV; 
	static String             retCodeV;

	static String[] fieldGroupInConfPropsV=null;

	public  Object[] doValidate (			
			XSSFRow  headRowV,
			XSSFRow  rowDataV,
			String[] fieldListInConfPropsV,
			int      NoCV
			) {

		Utils utilsV = new Utils();
		okToProceedV=true;
		retCodeV="";
		Object[] theValueRetV= { okToProceedV, retCodeV };

		try {
			fldConfVProps.load(new FileInputStream(fieldConfVProps));
		} catch (Exception esdf) {
			retCodeV="Error reading Field Configuration Property file";
			logger.error(retCodeV,esdf);
			okToProceedV=false;
		}

		/* 
		 * 
		 * 
		 * VALIDATE FIELD LIST ************************************************************* 
		 * 
		 * 
		 * */
		currentStepV="VALIDATE FIELD POSITION";

		for (int flPos=0; flPos<fieldListInConfPropsV.length; flPos++) {
			String fieldgroupFilterField=fldConfVProps.getProperty("field.group."+fieldListInConfPropsV[flPos],"NOT_FOUND").trim();							
			if (fieldgroupFilterField.equalsIgnoreCase("NOT_FOUND")) { 
				/* 
				 * 
				 * VALIDATE FIELD LIST - DATA FIELD IS AN ATTRIBUTE OR A REFERENCE ****************************************** 
				 * 
				 * */			

				int posAttr=utilsV.findPos(headRowV,fieldListInConfPropsV[flPos],NoCV);
				Object[] getCT = null;
				if (posAttr==-1) {
					retCodeV="["+currentStepV+"] Field Attr./Ref. "+ fieldListInConfPropsV[flPos]+" without position.";
					logger.error(retCodeV);
					okToProceedV=false;
				} else {
					// Validate the cell type					
					getCT=ValidateCellType(rowDataV, posAttr);
					if (!((boolean) getCT[1])) {
						retCodeV="["+currentStepV+"] Field Attr./Ref. "+ fieldListInConfPropsV[flPos]+" - The data type is not valid ["+getCT[0]+"]";
						logger.error(retCodeV);
						okToProceedV=false;	
					}					
				}
				if (okToProceedV) {
					logger.debug("["+currentStepV+"] Field Attr./Ref. "+ fieldListInConfPropsV[flPos]+" has position "+ posAttr+" - The data type is "+getCT[0]);
				}
			} else {
				/* 
				 * 
				 * VALIDATE DATA FIELD GROUP ****************************************** 
				 * 
				 * */				

				fieldGroupInConfPropsV=fieldgroupFilterField.split("\\|");
				String[] fieldGroupListOfField=fieldGroupInConfPropsV[0].split(",");
				for (int flMtx1=0; flMtx1<fieldGroupListOfField.length; flMtx1++) {									
					int posGroup=utilsV.findPos(headRowV,fieldGroupListOfField[flMtx1],NoCV);
					Object[] getCTFG = null;
					if (posGroup==-1) {
						retCodeV="["+currentStepV+"] Field Group ["+
								fieldGroupInConfPropsV[flPos]+"] Field "+ fieldGroupListOfField[flMtx1]+" without position.";
						logger.error(retCodeV);
						okToProceedV=false;
					}else {
						// Validate the cell type						
						getCTFG=ValidateCellType(rowDataV, posGroup);
						if (!((boolean) getCTFG[1])) {
							retCodeV="["+currentStepV+"] Field Group "+ fieldListInConfPropsV[flPos]+" - The cell type is not valid ["+getCTFG[0]+"]";
							logger.error(retCodeV);
							okToProceedV=false;	
						}
					}
					
					if (okToProceedV) {
					logger.debug("["+currentStepV+"] Field Group ["+
						fieldListInConfPropsV[flPos]+"] Field "+ fieldGroupListOfField[flMtx1]+" has position "+ posGroup+" - The Cell TYPE is "+getCTFG[0]);
					}
					
				}								
			}
			
			if (!okToProceedV) {
				break;
			}
			
		}

		theValueRetV[0]=okToProceedV;
		theValueRetV[1]=retCodeV;
		return theValueRetV;
	}

	public Object[] ValidateCellType(XSSFRow rowDataVCT, int PosValCellType) {

		// cell must be expected type
		String theCellTypeString="";
		boolean resultVCT=true;
		Object[] resultVCTArr = {theCellTypeString,resultVCT};
		XSSFCell cellData = rowDataVCT.getCell(PosValCellType);
		if (!(cellData==null || 
				cellData.getCellType()==XSSFCell.CELL_TYPE_BLANK)) {
			int theCellType=cellData.getCellType();
			switch (theCellType) {
			case 1: // STRING
				theCellTypeString="[String]";
				break; 
			case 0: // NUMERIC or DATE
				if (DateUtil.isCellDateFormatted(cellData)) {  
					theCellTypeString="[Date]";
					break; 
				} else { 
					theCellTypeString="[Numeric]";
				} 
				break; 
			case 4: // BOOLEAN
				theCellTypeString="[Boolean]";
				break; 				
			default: 
				theCellTypeString="["+theCellType+" - NOT VALID]";
				resultVCT=false;
			}
		} else {
			theCellTypeString="[NO FIELD VALUE]";
		}
		resultVCTArr[0]=theCellTypeString;
		resultVCTArr[1]=resultVCT;
		return resultVCTArr;
	}

	
	public Object[] ValidateDataType(String FieldName, String FieldValue) {

		boolean okToProceedToReturn=true;
		String fieldListValueToReturn=FieldValue;
		String retMesgVDT="";
		Object[] ValidateDataTypeObjToReturn= { okToProceedToReturn , fieldListValueToReturn , retMesgVDT };
		
		try {
			fldConfVProps.load(new FileInputStream(fieldConfVProps));
		} catch (Exception esdf) {
			logger.error("Error reading Field Configuration Property file",esdf);
			okToProceedToReturn=false;
			fieldListValueToReturn=FieldValue;
		}

		
		String isAnInt=fldConfVProps.getProperty("field.datatype.integer."+FieldName,"NOT_FOUND").trim();
		if (!(isAnInt.equalsIgnoreCase("NOT_FOUND"))) {
			/*
			 *  the field is an integer
			 */
			logger.info("The Field "+ FieldName+" is an Integer");
			if (FieldValue.matches("^\\d+$")) {
				okToProceedToReturn=true;
				fieldListValueToReturn=FieldValue;
			} else {
				retMesgVDT="The Field value for "+ FieldName+
						" is not a valid number";
				logger.error(retMesgVDT);
				okToProceedToReturn=false;
				fieldListValueToReturn=FieldValue;
			}
		}

		
		String isADate=fldConfVProps.getProperty("field.datatype.date."+FieldName,"NOT_FOUND").trim();
		if (!(isADate.equalsIgnoreCase("NOT_FOUND"))) {
			/*
			 *  the field is a Date
			 */			
			String[] isADateArr=isADate.split(",");
			String OLD_FORMAT = isADateArr[0];
			String NEW_FORMAT = isADateArr[1];
			logger.info("The Field "+ FieldName+
					" is a Date. File data Format is "+OLD_FORMAT+
					" - New data Format is "+NEW_FORMAT);
			String oldDateString = FieldValue;
			String newDateString;			
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
				sdf.setLenient(false);
				Date d = (Date) sdf.parse(oldDateString);
				sdf.applyPattern(NEW_FORMAT);
				newDateString = sdf.format(d);
				okToProceedToReturn=true;
				fieldListValueToReturn=newDateString;
			} catch (Exception e) {
				retMesgVDT="Field "+ FieldName+
						" is not valid according to  "+OLD_FORMAT+" pattern.";
				logger.error(retMesgVDT);
				okToProceedToReturn=false;
				fieldListValueToReturn=FieldValue;
			}
		} 

		ValidateDataTypeObjToReturn[0]=okToProceedToReturn;
		ValidateDataTypeObjToReturn[1]=fieldListValueToReturn;
		ValidateDataTypeObjToReturn[2]=retMesgVDT;
		return ValidateDataTypeObjToReturn;	
	}
}
