package interlakeprogrammingclub.bsdgradechecker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



public class Login extends Activity implements View.OnClickListener {

    public boolean debugging = true;
    private String username;
    private String password;

    private EditText unameField;
    private EditText passwordField;
    private ImageButton loginButton;


    private ProgressBar spinner;
    private LinearLayout screen;

    /*
    GradeData Documentation (Key and then what it specifys):
    p1-1st period class name
    p2-2nd period class name etc.
     */
    private SharedPreferences gradeData;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        loginButton = (ImageButton) findViewById(R.id.loginButton);
        spinner = (ProgressBar) findViewById(R.id.loginSpinner);
        screen = (LinearLayout) findViewById(R.id.loginForm);

        loginButton.setOnClickListener(this);
        spinner.setVisibility(View.GONE);

        settings = getSharedPreferences("settings", MODE_PRIVATE);
        gradeData = getSharedPreferences("grades", MODE_PRIVATE);

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
            login( username, password);

                settings.edit().putString("username",username);
                settings.edit().putString("password",password);

        }
    }

    //Not sure what to do here
    public void login(String uname, String pass){
        new LoginProtocol(this).execute(uname, pass);
    }

    private class LoginProtocol extends AsyncTask<String,Integer,String>{
        private Context c;
        public LoginProtocol(Context c){
            this.c = c;
        }
        @Override
        protected void onPreExecute(){
            spinner.setVisibility(View.VISIBLE);
        }


        //Params[0] is username
        //Params[1] is password
        @Override
        protected String doInBackground(String... params) {
            try {
                //Get basic cookie data and login tokens from aspen
                Connection con = Jsoup.connect("https://aspen.bsd405.org/aspen/logon.do")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
                Connection.Response res = con.execute();
                Document doc = res.parse();

                Map<String, String> formData = getFormData(doc);

                //Obtain jsessionid
                String jsess = doc.getElementsByAttributeValueMatching("name", "logonForm").first().attr("action");
                jsess = jsess.substring(jsess.indexOf("=")+1);
                gradeData.edit().putString("jsess",
                        jsess).apply();


                //Submit newfound data obtained in previous step
                Jsoup.connect("https://aspen.bsd405.org/aspen/logon.do")
                        .method(Connection.Method.POST)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID",jsess)
                        .data("username", params[0], "password", params[1])
                        .data(formData).execute();

                //Test whether or not jsessionid works
                Jsoup.connect("https://aspen.bsd405.org/aspen/home.do")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID",jsess)
                        .execute().parse();


                //Get homepage html data for future parsing
                doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassList.do?navkey=academics.classes.list")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", jsess)
                        .execute().parse();

                //Obtain each class that the user is taking/update that list
                Elements dgrid = doc.getElementById("dataGrid").getElementsByTag("table").first()
                        .getElementsByTag("tbody").first()
                        .getElementsByClass("listCell");


                for(int i = 0; i < dgrid.size(); i++){
                    //Record class name into sharedprefs
                    Element atag=dgrid.get(i).getElementsByTag("td").get(1).getElementsByTag("a").first();
                    String href=atag.attr("href");

                    String key=href.substring(href.indexOf("SS"),href.length()-2);
                    gradeData.edit().putString("k" + (i+1),
                            ""+key).apply();
                    gradeData.edit().putString("p" + (i + 1),
                            atag.html()).apply();

                    Element grade=dgrid.get(i).getElementsByTag("td").get(7).getElementsByTag("div").first();
                    if(grade!=null){
                        String avg=grade.html().substring(46);
                        Log.d("lol", avg);
                        gradeData.edit().putString("g" + (i+1),
                                avg).apply();

                    }
                    else{
                        gradeData.edit().putString("g" + (i+1),
                                "").apply();
                    }


                }

                /*
                Future notes for people who might want to log into aspen (aka me)
                    After you log in and are at the portalclasslist thing, there are more form elements
                    You submit more form data based on the form elements in the html to get access to your class grades
                    Once you get access, you gotta submit yet more form data with the get request to get the webpage you want (it is a navkey string)
                 */
            }
            catch(IOException e){
                Log.d("Exception", e.toString());
            }
            return null;
        }

        /*
         * Pull average grades from that table when you click academics
         * To my knowledge this part works fine
         */
        private void pullAvgGrades(Document doc, int i){

        }

        private Map<String, String> getFormData(final Document doc){
            Map<String, String> fd = new HashMap<String, String>();
            Elements forms = doc.getElementsByTag("input");

            for(int j = 0; j < forms.size(); j++){
                Element f = forms.get(j);
                if(f.attr("type").contentEquals("hidden")){
                    fd.put(f.attr("name"), f.attr("value"));
                }
            }

            return fd;
        }
        @Override
        protected void onPostExecute(String result){
            spinner.setVisibility(View.GONE);
            startActivity(new Intent(c, GradesViewerActivity.class));
        }
    }

}
