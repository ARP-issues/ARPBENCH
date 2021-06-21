package io.evercam.androidapp;

import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;

import io.evercam.androidapp.utils.Constants;

public class SimpleWebActivity extends WebActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_web);

        setUpDefaultToolbar();

        if(bundle != null)
        {
            getSupportActionBar().hide();
            loadPage();
        }
        else
        {
            finish();
        }
    }

    @Override
    protected void loadPage()
    {
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(getWebViewClient());

        //Enable DevTool debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        String url = bundle.getString(Constants.BUNDLE_KEY_URL);
        webView.loadUrl(url);
    }

//    private void test()
//    {
//        InputStream inputStream = getApplicationContext().getAssets().open("fonts/myfontasset.ttf");
//        WebResourceResponse response = null;
//        String encoding= "UTF-8";
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            int statusCode = 200;
//            String reasonPhase = "OK";
//            Map<String, String> responseHeaders = new HashMap<String, String>();
//            responseHeaders.put("Access-Control-Allow-Origin","*");
//            response = new WebResourceResponse("font/ttf", encoding, statusCode, reasonPhase, responseHeaders, inputStream);
//        } else {
//            response = new WebResourceResponse("font/ttf", encoding, inputStream);
//        }
//    }
}
