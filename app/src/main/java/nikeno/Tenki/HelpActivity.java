package nikeno.Tenki;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		WebView webView = findViewById(R.id.webView);
		webView.loadUrl("file:///android_asset/help.html");
	}  
}
