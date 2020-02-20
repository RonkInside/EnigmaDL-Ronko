package com.nttdata.enel.EnigmaInt;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class PutAction {

	protected static Logger   logger             = Logger.getLogger("com/nttdata/enel/EnigmaInt");

	public Object[] theRequest
	(CloseableHttpClient client, 
			String ProxyHost, String ProxyPort, 
			String destTableName, 
			String putData,
			String theSysID,
			String returnValueField)
					throws HttpException, IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException  
	{
		logger.info("++++++++++++++++++++ START PUT ACTION Request - Sys ID "+theSysID+" ++++++++++++++++++++");
		Object[] theValueRet= { "9999" , "" };

		try {
			CloseableHttpResponse responsePut = null;
			HttpPut httpput = new HttpPut(
					"https://"+ProxyHost+":"+ProxyPort+"/api/now/v2/table/"+
							destTableName+"/"+theSysID+"?sysparm_fields=sys_id");

			try {
				httpput.setHeader("Accept", "application/json");
				httpput.setHeader("Content-Type", "application/json");

				logger.info("PUT ACTION - Table: "+destTableName+ " - Request: "+putData); //DATA
				httpput.setEntity(new ByteArrayEntity(putData.getBytes("utf-8")));

				logger.debug("PUT ACTION - RequestLine: " +httpput.getRequestLine());
				responsePut = client.execute(httpput);

				// rc
				StatusLine statusLinePut = responsePut.getStatusLine();
				int StatusCodeIntPut = statusLinePut.getStatusCode();
				theValueRet[0] = StatusCodeIntPut;
				logger.info("PUT ACTION - Status Line: " +statusLinePut + " - Status Code: " + StatusCodeIntPut);

				// rb
				String responsePostBody = EntityUtils.toString(responsePut.getEntity());
				if (StatusCodeIntPut<300) {
					JSONObject obj = new JSONObject(responsePostBody);
					theValueRet[1] = obj.getJSONObject("result").getString(returnValueField);
					logger.info("PUT ACTION - Body: " + theValueRet[1]);
				} else {
					logger.info("PUT ACTION - Body (Error): "+ responsePostBody);
					theValueRet[2] = responsePostBody;						
				}
				responsePut.close();
			} finally {
				responsePut.close();
			}
		} finally {
			//logger.debug("----------------- finally close client Put -----------------");
			//client.close();
		}
		logger.info("++++++++++++++++++++ End Put Request ++++++++++++++++++++");
		return theValueRet;
	}
}


