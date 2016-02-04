package interlakeprogrammingclub.bsdgradechecker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AssignViewerActivity extends AppCompatActivity {

    SharedPreferences gradeData;
    ProgressBar spinner;
    ListView listView;

    String period;
    String jsess;
    ArrayList<String> scores=new ArrayList<String>();
    String[] assignments;
    Activity a=this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_viewer);
        spinner = (ProgressBar) findViewById(R.id.loading);


        period= getIntent().getStringExtra("period");

        spinner.setVisibility(View.GONE);
        listView = (ListView) findViewById(R.id.list2);
        find();


        Log.d("shoot","fuuu");
        if(assignments==null){
            Log.d("shoot","sigh");
        }
        else{
            Log.d("working","good");
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, assignments);
            listView.setAdapter(adapter);
        }



    }
    public void find(){
        new LoginProtocol(this).execute();
    }
    private class LoginProtocol extends AsyncTask<String,Integer,String> {
        private Context c;

        public LoginProtocol(Context c) {
            this.c = c;
        }

        @Override
        protected void onPreExecute() {
            spinner.setVisibility(View.VISIBLE);
        }


        //Params[0] is username
        //Params[1] is password
        @Override
        protected String doInBackground(String... params) {
            gradeData = getSharedPreferences("grades", MODE_PRIVATE);
            try {
                jsess=gradeData.getString("jsess",null);
                //Get basic cookie data and login tokens from aspen
                Connection con = Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassList.do?navkey=academics.classes.list")
                        .method(Connection.Method.GET)
                        .cookie("JSESSIONID", jsess)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
                Connection.Response res = con.execute();

                if(res==null){
                    Log.d("DAMMIT", "DAMMIT");
                }

                Document doc = res.parse();

                Map<String, String> formData = getFormData(doc);
                formData.put("userEvent", "2100");

                String key=gradeData.getString("k"+(Integer.parseInt(period)+1),null);

                Log.d("lol",key);

                formData.put("userParam", key);

                //Obtain jsessionid
                /*
                String jsess = doc.getElementsByAttributeValueMatching("name", "studentFilteredListForm").first().attr("action");
                jsess = jsess.substring(jsess.indexOf("=") + 1);
                */


                //Submit newfound data obtained in previous step
                Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassList.do?navkey=academics.classes.list")
                        .method(Connection.Method.POST)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", jsess)
                        .data(formData).execute();

                //Test whether or not jsessionid works
                Jsoup.connect("https://aspen.bsd405.org/aspen/portalClassDetail.do?navkey=academics.classes.list.detail")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", jsess)
                        .execute().parse();


                //Get homepage html data for future parsing
                doc = Jsoup.connect("https://aspen.bsd405.org/aspen/portalAssignmentList.do?navkey=academics.classes.list.gcd")
                        .method(Connection.Method.GET)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
                        .cookie("JSESSIONID", jsess)
                        .execute().parse();

                //Obtain each class that the user is taking/update that list
                Elements dgrid = doc.getElementById("dataGrid").getElementsByTag("table").first()
                        .getElementsByTag("tbody").first()
                        .getElementsByClass("listCell");


                for (int i = 0; i < dgrid.size(); i++) {
                    //Record class name into sharedprefs
                    Elements check = dgrid.get(i).getElementsByTag("td").get(0).getElementsByTag("div");
                    if(check.first()!=null&&check.first().html().equals("No matching records")){

                        Log.d("sigh","worked");
                        break;
                    }


                    Element atag = dgrid.get(i).getElementsByTag("td").get(1).getElementsByTag("a").first();
                    String assignment=atag.html();
                    if(dgrid.get(i).getElementsByTag("td").get(5).getElementsByTag("table").first()!=null) {


                        Elements grade = dgrid.get(i).getElementsByTag("td").get(5).getElementsByTag("table").first().getElementsByTag("tbody").first().getElementsByTag("tr").first().getElementsByTag("td");

                        if (grade.html().equals("<strong>No score</strong>")) {
                            Log.d("sigh", i + assignment);
                        } else {

                            String percent = grade.first().getElementsByTag("div").first().getElementsByTag("span").first().html();
                            Log.d("shit", percent);
                            scores.add(percent + " " + assignment);

                        }
                    }



                }
                assignments=new String[scores.size()];
                for(int i=0; i<scores.size(); i++){
                    assignments[i]=scores.get(i);

                }




                /*
                Future notes for people who might want to log into aspen (aka me)
                    After you log in and are at the portalclasslist thing, there are more form elements
                    You submit more form data based on the form elements in the html to get access to your class grades
                    Once you get access, you gotta submit yet more form data with the get request to get the webpage you want (it is a navkey string)
                 */
            } catch (IOException e) {
                Log.d("dammit", "exception");

            }
            return null;
        }
        protected void onPostExecute(String result){
            spinner.setVisibility(View.GONE);
            Intent intent = new Intent(a, AssignViewerActivity2.class);
            intent.putExtra("assignments", assignments);
            startActivity(intent);
        }
    }

        /*
         * Pull average grades from that table when you click academics
         * To my knowledge this part works fine
         */
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


}
