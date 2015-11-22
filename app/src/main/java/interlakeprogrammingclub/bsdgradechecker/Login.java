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
    private Button loginButton;
    private CheckBox stayLoggedIn;
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

        //Initialize the views that we will use
        unameField = (EditText) findViewById(R.id.unameField);
        passwordField = (EditText) findViewById(R.id.passwordField);
        loginButton = (Button) findViewById(R.id.loginButton);
        stayLoggedIn = (CheckBox) findViewById(R.id.keepMeLoggedIn);
        spinner = (ProgressBar) findViewById(R.id.loginSpinner);
        screen = (LinearLayout) findViewById(R.id.loginForm);
        passwordField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            onClick(loginButton);
                            break;
                    }
                }
                return false;
            }
        });

        loginButton.setOnClickListener(this);
        spinner.setVisibility(View.GONE);

        settings = getSharedPreferences("settings", MODE_PRIVATE);
        gradeData = getSharedPreferences("grades", MODE_PRIVATE);
        if(debugging)
            gradeData.edit().clear().commit(); //PURELY FOR DEBUGGING!!! TAKE THIS OUT IF NOT DEBUGGING
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
            username = unameField.getText().toString();
            password = passwordField.getText().toString();
            login(username, password);
            if(stayLoggedIn.isChecked()){
                settings.edit().putString("username",username);
                settings.edit().putString("password",password);
            }
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
                Map<String, String> formData = new HashMap<String, String>();
                Map<String, String> cookies = res.cookies();
                String vb = doc.getElementsByAttributeValue("name", "org.apache.struts.taglib.html.TOKEN").first().attr("value");
                //Obtain jsessionid
                String jsess = doc.getElementsByAttributeValueMatching("name", "logonForm").first().attr("action");
                jsess = jsess.substring(jsess.indexOf("=")+1);
                Elements form = doc.getElementsByTag("input");
                for(int i = 0; i < form.size() - 3; i++ ){
                    formData.put(form.get(i).attr("name"), form.get(i).attr("value"));
                }

                //Submit newfound data obtained in previous step
                Jsoup.connect("https://aspen.bsd405.org/aspen/logon.do")
                        .method(Connection.Method.POST)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID",jsess)
                        .data("username", params[0], "password", params[1])
                        .data(formData);

                //Get homepage html data for future parsing
                doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassList.do")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", jsess)
                        .data("navkey", "academics.classes.list")
                        .execute().parse();

                //Obtain each class that the user is taking/update that list
                Elements dgrid = doc.getElementById("dataGrid").getElementsByTag("table").first()
                        .getElementsByTag("tbody").first()
                        .getElementsByClass("listCell");


                for(int i = 0; i < dgrid.size(); i++){
                    //Record class name into sharedprefs
                    Element atag=dgrid.get(i).getElementsByTag("td").get(1).getElementsByTag("a").first();
                    gradeData.edit().putString("p" + (i+1),
                            atag.html()).apply();

                    //Record class assignments/overall grades here

                    //Find the userParam so that aspen knows which class I want to view
                    Map<String, String> fd = new HashMap<String, String>();
                    String up = atag.attr("href");
                    up = up.substring(0,up.lastIndexOf("'"));
                    up = up.substring(up.lastIndexOf("'") + 1); //Because the first character is included, which is why 1 is added to it
                    fd.put("userParam", up);

                    //Input the formdata again for the new thing...
                    Elements forms = doc.getElementsByTag("input");

                    for(int j = 0; j < forms.size(); j++){
                        Element f = forms.get(j);
                        if(f.attr("type").contentEquals("hidden")){
                           fd.put(f.attr("name"), f.attr("value"));
                        }
                    }

                    //Update the server on what data I want to see
                    Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassList.do")
                            .method(Connection.Method.POST)
                            .data(fd)
                            .cookie("JSESSIONID", jsess)
                            .execute();

                    //Get the data that I want to see about the class
                    doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassDetail.do")
                            .method(Connection.Method.GET)
                            .cookie("JSESSIONID", jsess)
                            .data("navkey", "academics.classes.list.detail")
                            .execute().parse();
                    //At this point in the code, the doc is the html of your grades in the period in which this loop is iterating on
                    Elements datagrid = doc.getElementsByAttributeValue("id", "dataGrid");
                    Element attendance = datagrid.get(0);
                    pullAvgGrades(doc, i); //Pull the average grades from the datatable with the id "dataGrid"
                    doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalAssignmentList.do")
                            .method(Connection.Method.GET)
                            .cookie("JSESSIONID", jsess)
                            .data("navkey", "academics.classes.list.gcd")
                            .execute().parse();
                    fd.clear();
                    forms = doc.getElementsByTag("input");
                    for(int j = 0; j < forms.size(); j++){
                        Element f = forms.get(j);
                        if(f.attr("type").contentEquals("hidden")){
                            fd.put(f.attr("name"), f.attr("value"));
                        }
                    }
                    for(int j = 1; j <= 4; j++){
                        doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalAssignmentList.do")
                                .method(Connection.Method.POST)
                                .cookie("JSESSIONID", jsess)
                                .data("gradeTermOid", doc.getElementsContainingText("MP" + j).attr("value"))
                                .data(fd)
                                .execute().parse();
                        //At this stage, doc is the html of the list of all your assignments in the jth quarter
                        Elements assignments = doc.getElementById("dataGrid").getElementsByClass("listCell");
                        for(int k = 0; k < assignments.size(); k++){
                            if(assignments.get(k).children().size() < 3){
                                break;
                            }
                            gradeData.edit().putString("p" + (i + 1) + " assignment" + k + " name q" + j,
                                    assignments.get(k).getElementsByTag("td").get(1).child(0).html()).apply();
                            gradeData.edit().putString("p" + (i + 1) + " assignment" + k + " category q" + j,
                                    assignments.get(k).getElementsByTag("td").get(2).html()).apply();
                            gradeData.edit().putString("p" + (i + 1) + " assignment" + k + " startdate q" + j,
                                    assignments.get(k).getElementsByTag("td").get(3).html()).apply();
                            gradeData.edit().putString("p" + (i + 1) + " assignment" + k + " endDate q" + j,
                                    assignments.get(k).getElementsByTag("td").get(4).html()).apply();
                            gradeData.edit().putString("p" + (i + 1) + " assignment" + k + " score q" + j,
                                    assignments.get(k).getElementsByTag("td").get(7).html()).apply();
                            gradeData.edit().putString("p" + (i+1) + " assignment" + k + " feedBack q" + j,
                                    assignments.get(k).getElementsByTag("td").last().html()).apply();
                        }
                    }

                    System.out.println("diditowrk");// This line is purely for debugging purposes, and has no value in the app at all.
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
            Element avgGradeTable = doc.getElementsByAttributeValue("id", "dataGrid").get(1);
            Elements categories = avgGradeTable.getElementsByClass("listCell");
            for(int j = 0; j < categories.size() - 1/* I just happen to know that the last one is useless*/ ; j++){
                gradeData.edit().putString("p" + (i+1) + " name " + j + "category",
                        categories.get(j).child(0).html()).apply();
                gradeData.edit().putString("p" + (i+1) + " weight "+ j + "category",
                        categories.get(j).child(1).html()).apply();
                gradeData.edit().putString("p" + (i+1) + " q1 "+ j + "category",
                        categories.get(j).child(2).html()).apply();
                gradeData.edit().putString("p" + (i+1) + " q2 "+ j + "category",
                        categories.get(j).child(3).html()).apply();
                gradeData.edit().putString("p" + (i+1) + " q3 "+ j + "category",
                        categories.get(j).child(4).html()).apply();
                gradeData.edit().putString("p" + (i+1) + " q4 "+ j + "category",
                        categories.get(j).child(5).html()).apply();
            }

            //Get quarter averages if not on previous table
            Elements es = avgGradeTable.getElementsByTag("tr");
            for(int j = 2; j < 6; j++) {
                gradeData.edit().putString("p" + (i + 1) + " gradeBookAverageQ" + (j-1),
                        es.get(es.size() - 2).child(j).html()).apply();
                gradeData.edit().putString("p" + (i + 1) + " postedGradeQ" + (j-1),
                        es.get(es.size() - 1).child(j).html()).apply();
            }

            //Get semester averages for grades
            Element s1 = doc.getElementsContainingOwnText("Sem. 1 Current Grade").first();
            gradeData.edit().putString("p" + (i+1) + " gradeBookAverageS1",
                    s1.parent().child(1).html()).apply();
            Element s2 = doc.getElementsContainingOwnText("Sem. 2 Current Grade").first();
            gradeData.edit().putString("p" + (i+1) + " gradeBookAverageS2",
                    s2.parent().child(1).html()).apply();
        }

        @Override
        protected void onPostExecute(String result){
            spinner.setVisibility(View.GONE);
            startActivity(new Intent(c, GradesViewerActivity.class));
        }
    }

}
