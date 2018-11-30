package org.elastos.carrier.demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.HttpUrl;
import okhttp3.MediaType;

public class Transaction{
	String ReceiverAddr;
	String SenderAddr;
    double Amount;
	String PrivateKey;
	String MetaData;

	public Transaction(String rcv_addr, String tx_addr, String pv_key, double amt, String mt_data){
		this.ReceiverAddr = rcv_addr;
		this.SenderAddr = tx_addr;
		this.Amount = amt;
		this.PrivateKey = pv_key;
		this.MetaData = mt_data;
		
	}
	
	//Returns the Tx ID of the transaction or null if the transaction fails
	String Send() throws IOException {
		final OkHttpClient client = new OkHttpClient();
		String data = "{"+
        "\"sender\":["+
            "{"+
                "\"address\":\"" + this.SenderAddr + "\","+
                "\"privateKey\":\"" + this.PrivateKey + "\""+
            "}"+
        "],"+
        "\"memo\":\"" + this.MetaData + "\","+
        "\"receiver\":["+
            "{"+
                "\"address\":\"" + this.SenderAddr + "\","+
                "\"amount\":\"" + this.Amount + "\""+
            "}" +
        "]" +
    "}";
		final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(JSON, data);
		Request request = new Request.Builder()
		.url("http://hw-ela-api-test.elastos.org/api/1/transfer")
		.post(body)
		.build();
		Response response = client.newCall(request).execute();
		String responseStr = response.toString();
		int index = responseStr.indexOf("\"result\":\"");
		if(index < 0)
			return null;
		int endIndex = responseStr.indexOf("\",") - 1;
		String tx_Id = responseStr.substring(index, endIndex);
		return tx_Id;
	}

	// returns a map (key/value pairs: RxAddress, Amount, hexMetadata), if not transaction found returns null
	static Map GetTxById(String TxId) throws IOException, NullPointerException{
		OkHttpClient client = new OkHttpClient();
        	HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("hw-ela-api-test.elastos.org")
                .addPathSegment("api")
                .addPathSegment("1")
                .addPathSegment("tx")
                .addPathSegment(TxId)
                .build();

       		System.out.println(httpUrl.toString());

        	Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .url(httpUrl)
                .build();

        	Response response = client.newCall(requesthttp).execute();
		Map MyDict = new HashMap(); 
		try
		{
			final JSONObject obj = new JSONObject(response.toString());

    		final JSONArray vout = obj.getJSONArray("vout");
    		final int n = vout.length();
		if(n <= 0)
			return null;
    		for (int i = 0; i < n; ++i) {
      			final JSONObject person = vout.getJSONObject(i);
      			MyDict.put("RxAddress", person.getInt("address"));
			MyDict.put("Amount", person.getInt("value"));
      			break;
		}
		final JSONArray attributes = obj.getJSONArray("attributes");
    		final int len = attributes.length();
    		for (int i = 0; i < len; ++i) {
      			final JSONObject data = vout.getJSONObject(i);
      			MyDict.put("hexMetadata", data.getInt("data"));
      			break;
		}
        	return MyDict;
		}catch (Exception e){

		}
		return null;
	}

	// if the returned balance is less than 0 then check the address or network
	static double GetBalance(String Addr) throws IOException, NullPointerException{
		OkHttpClient client = new OkHttpClient();
        	HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("hw-ela-api-test.elastos.org")
                .addPathSegment("api")
                .addPathSegment("1")
                .addPathSegment("balance")
                .addPathSegment(Addr)
                .build();

       		System.out.println(httpUrl.toString());

        	Request requesthttp = new Request.Builder()
                .addHeader("accept", "application/json")
                .url(httpUrl)
                .build();

        	Response response = client.newCall(requesthttp).execute();
		String responseStr = response.toString();
		int index = responseStr.indexOf("\"result\":\"");
		if(index < 0)
			return -1.0;
		int endIndex = responseStr.indexOf("\",") - 1;
		String amount = responseStr.substring(index, endIndex);
		return Double.parseDouble(amount);
	}

	
}


