package interlakeprogrammingclub.bsdgradechecker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class Login extends Activity implements View.OnClickListener {

    private String username;
    private String password;

    private EditText unameField;
    private EditText passwordField;
    private Button loginButton;
    private CheckBox stayLoggedIn;
    private ProgressBar spinner;
    private LinearLayout screen;

    //Note: these shared preferences will probably be used for everything. That is because I think
    //that this app will be small
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Initialize the views that we will use
        unameField = (EditText) findViewById(R.id.unameField);
        passwordField = (EditText) findViewById(R.id.passwordField);
        loginButton = (Button) findViewById(R.id.loginButton);
        stayLoggedIn = (CheckBox) findViewById(R.id.keepMeLoggedIn);
        spinner = (ProgressBar) findViewById(R.id.loginSpinner);
        screen = (LinearLayout) findViewById(R.id.loginForm);

        loginButton.setOnClickListener(this);
        spinner.setVisibility(View.GONE);

        settings = getSharedPreferences("settings", MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v == loginButton){
            if(stayLoggedIn.isChecked()){
                settings.edit().putString("username",username);
                settings.edit().putString("password",password);
            }
            username = unameField.getText().toString();
            password = passwordField.getText().toString();
            login(username,password);
        }
    }

    //Not sure what to do here
    public void login(String uname, String pass){
        new LoginProtocol().execute();
    }

    private class LoginProtocol extends AsyncTask<String,Integer,String>{
        @Override
        protected void onPreExecute(){
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String html;
            html = getHTML();
            try {
                Document a = Jsoup.parse("https://aspen.bsd405.org/");
                String buffer = getCookie();
                String jsess = buffer.substring(buffer.indexOf("=") + 1, buffer.indexOf(";"));
                Connection con = Jsoup.connect("https://aspen.bsd405.org/aspen/logon.do")
                        .method(Connection.Method.POST)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", buffer.substring(buffer.indexOf("=") + 1, buffer.indexOf(";")))
                        .data("username", "s-xuch", "password", "pewdiepieduck");
                Connection.Response res = con.execute();
                Map<String, String> newCookies = res.cookies();
                Document doc = res.parse();
            }
            catch(IOException e){
                Log.d("Exception", e.toString());
            }
            return null;
        }

        private String getCookie(){
            try {
                HttpsURLConnection c = (HttpsURLConnection) (new URL("https://aspen.bsd405.org/aspen/logon.do")).openConnection();
                c.setRequestMethod("GET");
                c.setDoInput(true);
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");

                c.connect();
                Map<String, List<String>> buffer = c.getHeaderFields();
                String html = "";
                BufferedReader read = new BufferedReader(new InputStreamReader(c.getInputStream()));
                String b;
                while((b = read.readLine()) != null){
                    html += b;
                }
                return buffer.get("Set-Cookie").get(0);
            }
            catch(Exception e){
                Log.d("error", e.toString());
                return null;
            }
        }
        private String getHTML(){
            try {
                String output = "org.apache.struts.taglib.html.TOKEN=8c2cc97dcf02931f630ed3f77486e26b&userEvent=930&userParam=&operationId=&deploymentId=x2sis&scrollX=0&scrollY=0&formFocusField=username&mobile=false&SSOLoginDone=&username=" + "s-xuch" + "&password=" + "pewdiepieduck";

                //Set up the connection
                URL url = new URL("https://aspen.bsd405.org/aspen/logon.do");
                HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
                c.setDoInput(true);
                c.setDoOutput(true);
                c.setRequestMethod("POST");
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
                c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                c.setRequestProperty("Content-Length", Integer.toString(output.length()));
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                c.setRequestProperty("Accept-Encoding", "gzip, deflate");
                c.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
                c.setRequestProperty("Cache-Control", "max-age=0");
                c.setRequestProperty("Connection", "keep-alive");
                c.setRequestProperty("Cookie", "JSESSIONID=C2793475E98D6FDBAA9CBCF3583DA371; deploymentId=x2sis; __utma=261430046.705141245.1434430510.1441164336.1441239179.12; __utmz=261430046.1435202913.5.3.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided); _ga=GA1.3.705141245.1434430510");
                c.setRequestProperty("Host", "aspen.bsd405.org");
                c.setRequestProperty("Origin", "https://aspen.bsd405.org");
                c.setRequestProperty("Referer", "https://aspen.bsd405.org/aspen/logon.do");

                //Write the form data out
                OutputStream out = c.getOutputStream();
                out.write(output.getBytes("UTF-8"));
                out.flush();
                out.close();
                c.connect();
                System.out.println(c.getResponseCode());

                BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
                String iline = in.readLine();
                StringBuffer o = new StringBuffer();
                while (iline != null) {
                    o.append(iline + '\n');
                    iline = in.readLine();
                }
                return o.toString();
            }
            catch(Exception e){
                Log.d("error",e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            spinner.setVisibility(View.GONE);

//            startActivity(new Intent(Login.this, GradesViewerActivity.class));
        }
    }

}
