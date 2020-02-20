package com.nttdata.enel.EnigmaInt;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Properties;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class Utils {
	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");
	static Properties         fldConfProps       = new Properties();
	static String             fieldConfProps     = System.getenv("FIELDCONF");

	public int findPos 
	(XSSFRow theRow, String fieldNameToFind, int numberOfCells)  {
		int FieldPosOnSheet=-1;
		for (int kLhead=0; kLhead<numberOfCells; kLhead++) {
			XSSFCell cellkRow = theRow.getCell(kLhead);
			if (!(cellkRow==null || 
					cellkRow.getCellType()==XSSFCell.CELL_TYPE_BLANK ||
					cellkRow.getCellType()==XSSFCell.CELL_TYPE_ERROR)) {
				String FieldTextString = cellkRow.getStringCellValue().trim();
				if (FieldTextString.equalsIgnoreCase(fieldNameToFind)) {
					FieldPosOnSheet=kLhead;
					break;
				}
			}
		}	
		return FieldPosOnSheet;
	}

	public String getCellVal (XSSFCell theCell) {
		String tempG="";
		boolean tempB;

		try {
			if (!(theCell==null || 
					theCell.getCellType()==XSSFCell.CELL_TYPE_BLANK ||
					theCell.getCellType()==XSSFCell.CELL_TYPE_ERROR)) {
				switch (theCell.getCellType()) {
				case XSSFCell.CELL_TYPE_STRING: 
					tempG=theCell.getStringCellValue().trim().replaceAll("\\r\\n|\\r|\\n", " ");
					break;
				case XSSFCell.CELL_TYPE_NUMERIC :
					if (DateUtil.isCellDateFormatted(theCell)) {
						Date tempD=(Date) theCell.getDateCellValue();
						SimpleDateFormat simpleDateFormat = 
								new SimpleDateFormat("dd/MM/yyyy");
						tempG=simpleDateFormat.format(tempD);
					} else {
						tempG=(Long.valueOf((long) theCell.getNumericCellValue())).toString().trim();
					}
					break; 
				case XSSFCell.CELL_TYPE_BOOLEAN :
					tempB=theCell.getBooleanCellValue();
					if (tempB) {
						tempG="true";
					} else {
						tempG="false";
					}
					break;
				default:
					tempG=theCell.getStringCellValue().trim().replaceAll("\\r\\n|\\r|\\n", " ");
				}
			}
		} catch(Exception e) {
			logger.error("Get Cell Value - Exception " , e);
		}
		return tempG;
	}

	public Object[] getSysIDReference 
	(CloseableHttpClient httpClient,
			String proxyhost, String proxyport,
			String extRef,
			XSSFRow rowData,
			XSSFRow headRow,
			int NoC
			) throws UnsupportedEncodingException {

		boolean  okToProceedUtils=true;
		String   fieldValueRefRetGSIR="";
		String   retMesgGSIR="";
		Object[] theValueRet= { okToProceedUtils , fieldValueRefRetGSIR , retMesgGSIR };

		try {
			fldConfProps.load(new FileInputStream(fieldConfProps));
		} catch (Exception esdf) {
			logger.error("Error reading Field Configuration Property file",esdf);
		}

		String     getStringExtRef="";
		String[]   extRefArr=null;
		String[]   extRefFromField=null;
		String     extRefDestTable="";
		String[]   extRefDestTableField=null;

		String extRefVal=fldConfProps.getProperty("field.externalref."+extRef,"NOT_FOUND").trim();
		if (!(extRefVal.equalsIgnoreCase("NOT_FOUND"))) {
			logger.debug("+++++ External Reference: "+extRefVal);
			// values of extRef exists in configuration properties, field group is configured
			extRefArr=extRefVal.split("\\|");
			extRefFromField      = extRefArr[0].split(",");  // elements in the current fields
			extRefDestTable      = extRefArr[1];  // destination table
			extRefDestTableField = extRefArr[2].split(",");  // elements in the destination fields


			for (int i_extRefDestTableField=0; i_extRefDestTableField<extRefDestTableField.length; i_extRefDestTableField++) {
				String fieldListValueExtRef="";
				try {																
					fieldListValueExtRef=  // is the single value - current field
							getCellVal(rowData.getCell(findPos(headRow,extRefFromField[i_extRefDestTableField],NoC))).trim();				
				} catch (Exception ab) {
					logger.error("Group Field Get Cell Value return null.");	
				}
				logger.debug("Get the Sys ID Reference - STARTING WITH ELEMENT "+
						extRefFromField[i_extRefDestTableField]+" -> "+
						extRefDestTableField[i_extRefDestTableField]);
				if (!(fieldListValueExtRef==null || fieldListValueExtRef=="" || fieldListValueExtRef.isEmpty())) {
					// QUERY CONSTRUCTOR
					getStringExtRef=
							getStringExtRef+
							extRefDestTableField[i_extRefDestTableField].split(":")[0].trim()+
							SysParmsEqual()+
							URLEncoder.encode(fieldListValueExtRef.trim(),"UTF-8")+
							SysParmsAnd();					
				}
			}

			if (getStringExtRef.length()>1) {
				getStringExtRef=
						getStringExtRef.substring(0, getStringExtRef.length() - SysParmsAnd().length());

				try {
					// Execute Get Action
					Object[] fieldValueRefRetKT = null;
					GetAction getActionRef = new GetAction();
					fieldValueRefRetKT=getActionRef.theRequest(
							httpClient, proxyhost, proxyport, 
							extRefDestTable,getStringExtRef,"sys_id","Yes");
					int exitStatusCodeGetRefGSIR = (int) fieldValueRefRetKT[0];
					fieldValueRefRetGSIR         = fieldValueRefRetKT[1].toString().trim();
					okToProceedUtils             = (boolean) fieldValueRefRetKT[2];

					if (okToProceedUtils)  {
						if (exitStatusCodeGetRefGSIR < 300) {
							if (fieldValueRefRetGSIR==null || 
									fieldValueRefRetGSIR==""   || 
									fieldValueRefRetGSIR.toString().isEmpty()) {								
								okToProceedUtils=false;
								retMesgGSIR="The Sys ID Reference return value is null for "+extRef+" [value null]. Record Discarded.";
								logger.error(retMesgGSIR);								
							} else {
								logger.debug("The Sys ID Reference return Success"); 
							}			
						} else {
							okToProceedUtils=false;
							retMesgGSIR="The Sys ID Reference return value is null for "+extRef+" [Error from Get Request]. Record Discarded.";
							logger.error(retMesgGSIR);
						}
					} else {
						okToProceedUtils=false;
						retMesgGSIR="The Sys ID Reference return value is null for "+extRef+" [Error from Get Request]. Record Discarded.";
						logger.error(retMesgGSIR);					}
				} catch(Exception e) {
					okToProceedUtils=false;	
					retMesgGSIR="The Sys ID Reference return value is null for "+extRef+". Record Discarded.";
					logger.error("Get Sys ID Reference - Exception " , e);
				} 
			}
		}

		theValueRet[0]=okToProceedUtils;
		theValueRet[1]=fieldValueRefRetGSIR;
		theValueRet[2]=retMesgGSIR;
		return theValueRet;
	}

	public void writeRowDiscarded (
			XSSFSheet sheetDis,
			XSSFRow   rowToDis,
			int       RowNumCreateDis,
			int       NoCDis) {
		XSSFCell  cellDis;
		XSSFRow rowDiscarded = sheetDis.createRow(RowNumCreateDis);
		for (int cellNumDis=0; cellNumDis<NoCDis; cellNumDis++) {
			XSSFCell cellOrig = rowToDis.getCell(cellNumDis);
			if (!(cellOrig==null || 
					cellOrig.getCellType()==XSSFCell.CELL_TYPE_BLANK ||
					cellOrig.getCellType()==XSSFCell.CELL_TYPE_ERROR)) {
				switch (cellOrig.getCellType()) {
				case XSSFCell.CELL_TYPE_BOOLEAN:				
					cellDis = rowDiscarded.createCell(cellNumDis);
					cellDis.setCellType(XSSFCell.CELL_TYPE_BOOLEAN);
					cellDis.setCellValue(rowToDis.getCell(cellNumDis).getBooleanCellValue());
					break;
				case XSSFCell.CELL_TYPE_NUMERIC:
					cellDis = rowDiscarded.createCell(cellNumDis);
					cellDis.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
					cellDis.setCellValue(rowToDis.getCell(cellNumDis).getNumericCellValue());
					break;
				case XSSFCell.CELL_TYPE_STRING:
					cellDis = rowDiscarded.createCell(cellNumDis);
					cellDis.setCellType(XSSFCell.CELL_TYPE_STRING);
					cellDis.setCellValue(rowToDis.getCell(cellNumDis).getStringCellValue());
					break;
				} // case
			}  // getCellType
		}	// for		
	}		

	public void writeRowOut (
			XSSFSheet sheetOutP,
			int       Row_number, 
			String    Operation_Type, String    Exit_Status,			
			String    Dest_Table, String    Sys_ID, 
			String    Notes) {
		XSSFCell  cellOut;
		XSSFRow rowOut = sheetOutP.createRow(Row_number);
		cellOut = rowOut.createCell(0);
		cellOut.setCellValue(Operation_Type);
		cellOut = rowOut.createCell(1);
		cellOut.setCellValue(Exit_Status);
		cellOut = rowOut.createCell(2);
		cellOut.setCellValue(Dest_Table);
		cellOut = rowOut.createCell(3);
		cellOut.setCellValue(Sys_ID);
		cellOut = rowOut.createCell(4);
		cellOut.setCellValue(Notes);
	}	

	public boolean checkRowHasData(XSSFRow r) {

		boolean hasData = true;
		if (r == null) {
			hasData = false;
		} else {
			hasData = false;
			for (Cell c : r) {
				if (c.getCellType() != Cell.CELL_TYPE_BLANK) {
					hasData = true;
					break;
				}
			}
		}	
		return hasData;
	}

	public String PLNameName() {
		return "u_enc_picklist_name";
	}
	public String PLValueName() {
		return "u_enc_picklist_value";
	}
	public String SysParmsEqual() {
		return "%3D";
	}
	public String SysParmsAnd() {
		return "%5E";
	}

	public String[] setFieldListMultipleValuesSeparator(String fieldName, String fieldValue) {
		// if the field is configured to be splitted, return an Array of values splitted
		String[] fieldValueArr= { "" };
		try {
			fldConfProps.load(new FileInputStream(fieldConfProps));
		} catch (Exception esdf) {
			logger.error("Error reading Field Configuration Property file",esdf);
		}
		String multiplevalueConf=fldConfProps.getProperty("field.multiplevalue."+fieldName,"NOT_FOUND").trim();
		if (!(multiplevalueConf.equalsIgnoreCase("NOT_FOUND"))) {
			if (!(fieldValue==null || fieldValue=="")) {
				logger.info("The field "+fieldName+" is a multiple value field.");
				fieldValueArr=fieldValue.split(";(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				if (fieldValueArr.length==0) {
					fieldValueArr=fieldValue.split(":(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				}
				for (int ain=0; ain<fieldValueArr.length; ain++) {
					String newval=fieldValueArr[ain].replaceAll("\"", "");
					fieldValueArr[ain]=newval;
				}
			}	
		} else {
			fieldValueArr[0]=fieldValue;
		}
		return fieldValueArr;
	}
}
