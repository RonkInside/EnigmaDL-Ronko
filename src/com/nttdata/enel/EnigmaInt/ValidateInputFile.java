package com.nttdata.enel.EnigmaInt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class ValidateInputFile {
	protected static Logger logger = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	static Properties fldConfVProps = new Properties();

	static String currentStepV;

	static boolean okToProceedV;

	static String retCodeV;

	static String[] fieldGroupInConfPropsV = null;

	public Object[] doValidate(XSSFRow headRowV, XSSFRow rowDataV, String[] fieldListInConfPropsV, int NoCV) {
		Utils utilsV = new Utils();
		okToProceedV = true;
		retCodeV = "";
		Object[] theValueRetV = { Boolean.valueOf(okToProceedV), retCodeV };
		Object[] fieldConfPropRet = null;
		fieldConfPropRet = utilsV.loadFldConfProps();
		fldConfVProps = (Properties)fieldConfPropRet[0];
		okToProceedV = ((Boolean)fieldConfPropRet[1]).booleanValue();
		if (!okToProceedV) {
			retCodeV = "Error reading Field Configuration Property file";
			logger.error(retCodeV);
		}
		if (fieldListInConfPropsV != null) {
			currentStepV = "VALIDATE FIELD POSITION";
			for (int flPos = 0; flPos < fieldListInConfPropsV.length; flPos++) {
				String fieldgroupFilterField = fldConfVProps.getProperty("field.group." + fieldListInConfPropsV[flPos], "NOT_FOUND").trim();
				if (fieldgroupFilterField.equalsIgnoreCase("NOT_FOUND")) {
					int posAttr = utilsV.findPos(headRowV, fieldListInConfPropsV[flPos], NoCV);
					Object[] getCT = null;
					if (posAttr == -1) {
						retCodeV = "[" + currentStepV + "] Field Attr./Ref. " + fieldListInConfPropsV[flPos] + " without position.";
						logger.error(retCodeV);
						okToProceedV = false;
					} else {
						getCT = ValidateCellType(rowDataV, posAttr);
						if (!((Boolean)getCT[1]).booleanValue()) {
							retCodeV = "[" + currentStepV + "] Field Attr./Ref. " + fieldListInConfPropsV[flPos] + " - The data type is not valid [" + getCT[0] + "]";
							logger.error(retCodeV);
							okToProceedV = false;
						}
					}
					if (okToProceedV)
						logger.info("[" + currentStepV + "] Field Attr./Ref. " + fieldListInConfPropsV[flPos] + " has position " + posAttr + " - The data type is " + getCT[0]);
				} else {
					fieldGroupInConfPropsV = fieldgroupFilterField.split("\\|");
					String[] fieldGroupListOfField = fieldGroupInConfPropsV[0].split(",");
					for (int flMtx1 = 0; flMtx1 < fieldGroupListOfField.length; flMtx1++) {
						int posGroup = utilsV.findPos(headRowV, fieldGroupListOfField[flMtx1], NoCV);
						Object[] getCTFG = null;
						if (posGroup == -1) {
							retCodeV = "[" + currentStepV + "] Field Group [" +
									fieldGroupInConfPropsV[flPos] + "] Field " + fieldGroupListOfField[flMtx1] + " without position.";
							logger.error(retCodeV);
							okToProceedV = false;
						} else {
							getCTFG = ValidateCellType(rowDataV, posGroup);
							if (!((Boolean)getCTFG[1]).booleanValue()) {
								retCodeV = "[" + currentStepV + "] Field Group [" +
										fieldGroupInConfPropsV[flPos] + "]  Field " + fieldListInConfPropsV[flPos] + " - The data type is not valid [" + getCTFG[0] + "]";
								logger.error(retCodeV);
								okToProceedV = false;
							}
						}
						if (okToProceedV)
							logger.info("[" + currentStepV + "] Field Group [" +
									fieldListInConfPropsV[flPos] + "] Field " + fieldGroupListOfField[flMtx1] + " has position " + posGroup + " - The data type is " + getCTFG[0]);
					}
				}
				if (!okToProceedV)
					break;
			}
		}
		theValueRetV[0] = Boolean.valueOf(okToProceedV);
		theValueRetV[1] = retCodeV;
		return theValueRetV;
	}

	public Object[] ValidateCellType(XSSFRow rowDataVCT, int PosValCellType) {
		String theCellTypeString = "";
		boolean resultVCT = true;
		Object[] resultVCTArr = { theCellTypeString, Boolean.valueOf(resultVCT) };
		XSSFCell cellData = rowDataVCT.getCell(PosValCellType);
		if (cellData != null &&
				cellData.getCellType() != 3) {
			int theCellType = cellData.getCellType();
			switch (theCellType) {
				case 1:
					theCellTypeString = "[String]";
					resultVCTArr[0] = theCellTypeString;
					resultVCTArr[1] = Boolean.valueOf(resultVCT);
					return resultVCTArr;
				case 0:
					if (DateUtil.isCellDateFormatted((Cell)cellData)) {
						theCellTypeString = "[Date]";
					} else {
						theCellTypeString = "[Numeric]";
					}
					resultVCTArr[0] = theCellTypeString;
					resultVCTArr[1] = Boolean.valueOf(resultVCT);
					return resultVCTArr;
				case 4:
					theCellTypeString = "[Boolean]";
					resultVCTArr[0] = theCellTypeString;
					resultVCTArr[1] = Boolean.valueOf(resultVCT);
					return resultVCTArr;
			}
			theCellTypeString = "[" + theCellType + " - NOT VALID]";
			resultVCT = false;
		} else {
			theCellTypeString = "[NO FIELD VALUE]";
		}
		resultVCTArr[0] = theCellTypeString;
		resultVCTArr[1] = Boolean.valueOf(resultVCT);
		return resultVCTArr;
	}

	public Object[] ValidateDataType(String FieldName, String FieldValue) {
		boolean okToProceedToReturn = true;
		String fieldListValueToReturn = FieldValue;
		String retMesgVDT = "";
		Object[] ValidateDataTypeObjToReturn = { Boolean.valueOf(okToProceedToReturn), fieldListValueToReturn, retMesgVDT };
		Utils utilsV = new Utils();
		Object[] fieldConfPropRet = null;
		fieldConfPropRet = utilsV.loadFldConfProps();
		fldConfVProps = (Properties)fieldConfPropRet[0];
		okToProceedToReturn = ((Boolean)fieldConfPropRet[1]).booleanValue();
		if (!okToProceedToReturn) {
			retMesgVDT = "Error reading Field Configuration Property file";
			logger.error(retMesgVDT);
		}
		String isAnInt = fldConfVProps.getProperty("field.datatype.integer." + FieldName, "NOT_FOUND").trim();
		if (!isAnInt.equalsIgnoreCase("NOT_FOUND")) {
			logger.info("The Field " + FieldName + " is configured as Integer");
			try {
				double d = Double.parseDouble(FieldValue);
				okToProceedToReturn = true;
			} catch (NumberFormatException nfe) {
				retMesgVDT = "The Field value for " + FieldName +
						" is not a valid number";
				logger.error(retMesgVDT);
				okToProceedToReturn = false;
				fieldListValueToReturn = FieldValue;
			}
		}
		String isADate = fldConfVProps.getProperty("field.datatype.date." + FieldName, "NOT_FOUND").trim();
		if (!isADate.equalsIgnoreCase("NOT_FOUND")) {
			String[] isADateArr = isADate.split(",");
			String OLD_FORMAT = isADateArr[0];
			String NEW_FORMAT = isADateArr[1];
			logger.info("The Field " + FieldName +
					" is configured as Date, Date Format is " + OLD_FORMAT +
					" - New Date Format is " + NEW_FORMAT);
			String oldDateString = FieldValue;
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
				sdf.setLenient(false);
				Date d = sdf.parse(oldDateString);
				sdf.applyPattern(NEW_FORMAT);
				String newDateString = sdf.format(d);
				okToProceedToReturn = true;
				fieldListValueToReturn = newDateString;
			} catch (Exception e) {
				retMesgVDT = "Field " + FieldName +
						" is not valid according to  " + OLD_FORMAT + " pattern.";
				logger.error(retMesgVDT);
				okToProceedToReturn = false;
				fieldListValueToReturn = FieldValue;
			}
		}
		String isAnDecimal = fldConfVProps.getProperty("field.datatype.decimal." + FieldName, "NOT_FOUND").trim();
		if (!isAnDecimal.equalsIgnoreCase("NOT_FOUND")) {
			logger.info("The Field " + FieldName + " is configured as Decimal");
			if (FieldValue.matches("-?\\d+(\\.\\d+)?")) {
				okToProceedToReturn = true;
				fieldListValueToReturn = FieldValue;
			} else {
				retMesgVDT = "The Field value for " + FieldName +
						" is not a valid number";
				logger.error(retMesgVDT);
				okToProceedToReturn = false;
				fieldListValueToReturn = FieldValue;
			}
		}
		ValidateDataTypeObjToReturn[0] = Boolean.valueOf(okToProceedToReturn);
		ValidateDataTypeObjToReturn[1] = fieldListValueToReturn;
		ValidateDataTypeObjToReturn[2] = retMesgVDT;
		return ValidateDataTypeObjToReturn;
	}
}
