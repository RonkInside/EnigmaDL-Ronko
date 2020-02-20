package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class GetAction {

	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");
	
	public Object[] theRequest
	(CloseableHttpClient client, 
			String ProxyHost, String ProxyPort, 
			String destTableName, 
			String getData,
			String returnValueField,
			String getValueWithSysparm) 
					throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException  
	{
		logger.info("++++++++++++++++++++ Start Get Request ++++++++++++++++++++");
		logger.info("Table Name: " +destTableName+ " - Query: "+getData); //DATA

		int     exitStatusCodeGet=999;
		String  token  = "";
		boolean okToProceedGetAction=true;
		String  errorText="";
		Object[] theValueRet= { exitStatusCodeGet, token, okToProceedGetAction, errorText };

		try {
			StatusLine statusLineGet = null;
			CloseableHttpResponse responseGet = null;
			HttpEntity responseGetBody = null;

			HttpGet httpget = new HttpGet();
			if (getValueWithSysparm.equalsIgnoreCase("Yes")) {
				httpget = new HttpGet("https://"+ProxyHost+":"+ProxyPort+
						"/api/now/v2/table/"+destTableName+	"?sysparm_query="+ getData);				
			} else {
				httpget = new HttpGet("https://"+ProxyHost+":"+ProxyPort+
						"/api/now/v2/table/"+destTableName+	"?"+ getData);
			}
			try {
				httpget.setHeader("Accept", "application/json");
				httpget.setHeader("Content-Type", "application/json");

				responseGet       = client.execute(httpget);
				statusLineGet     = responseGet.getStatusLine();
				exitStatusCodeGet = statusLineGet.getStatusCode();
				theValueRet[0]    = (int) exitStatusCodeGet;
				responseGetBody   = responseGet.getEntity();

				if (responseGetBody != null) {
					String retSrc = "";
					JSONObject result   = null;
					JSONArray tokenList = null;
					JSONObject oj       = null;

					retSrc = EntityUtils.toString(responseGetBody); 
					if (exitStatusCodeGet < 300) {
						logger.debug("GET REQUEST - "+ statusLineGet + " - Exit Code: " + exitStatusCodeGet);
						result           = new JSONObject(retSrc);
						tokenList        = result.getJSONArray("result");
						int tokenListLen = tokenList.length();

						if (tokenListLen == 0) {
							logger.info("GET REQUEST ("+statusLineGet+") - "+returnValueField+" RETURN NO VALUE (token List: " +tokenListLen+")");
						}

						if (tokenListLen == 1) {
							oj = tokenList.getJSONObject(0);
							if(oj!=null) {
								token = oj.getString(returnValueField);  //value
								logger.info("GET REQUEST ("+exitStatusCodeGet+") - "+returnValueField+" HAS VALUE " + token + " (token List: "+tokenListLen+")");
								theValueRet[1]=token;
							} else {
								logger.info("GET REQUEST ("+exitStatusCodeGet+") - "+returnValueField+" RETURN NO VALUE (token List: " +tokenListLen+")");
							}
						}						

						if (tokenListLen > 1) {
							logger.warn("GET REQUEST ("+exitStatusCodeGet+") - "+returnValueField+" RETURN MULTIPLE VALUE " + token + " (token List: "+tokenListLen+")");
							theValueRet[2]=false;
						}

					}
					if (exitStatusCodeGet >= 300) {
						logger.error("GET REQUEST ("+exitStatusCodeGet+") - "+ statusLineGet + " - Exit Code: " + exitStatusCodeGet);
						logger.error(retSrc);
						theValueRet[3]=statusLineGet + " - " + retSrc;
						theValueRet[2]=false;
					}
				} else {
					logger.error("GET REQUEST ("+exitStatusCodeGet+") - Response Body NULL : "+ statusLineGet + " - Exit Code: " + exitStatusCodeGet);
					theValueRet[2]=false;
				}
				responseGet.close();				
			} finally {
				responseGet.close();
			}	
		} finally {
			//logger.debug("----------------- finally close client Get -----------------");
			//client.close();
		}
		logger.info("++++++++++++++++++++ End Get Request ++++++++++++++++++++");
		return theValueRet;
	}
}

