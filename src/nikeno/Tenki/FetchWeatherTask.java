package nikeno.Tenki;

import java.io.InputStream;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class FetchWeatherTask extends AsyncTask<String, Integer, String> {
	
	public String mResultMessage;
	
	void FetchWeatherTaxk() {
	
	}

	@Override
	protected String doInBackground(String... params) {
		String result = null;
		
		try {
			URL url = new URL(params[0]);
			Log.d("FetchTask", "url:"+url.toURI());
			
			InputStream is = url.openConnection().getInputStream();
			result = is.toString();
		}
		catch (Exception e) {
			mResultMessage = "ERROR: " + e.getMessage();
			e.printStackTrace();
		}
		return result;
	}
	
}
