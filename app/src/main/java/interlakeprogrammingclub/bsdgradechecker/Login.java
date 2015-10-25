package interlakeprogrammingclub.bsdgradechecker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class Login extends Activity implements View.OnClickListener {

    private String username;
    private String password;

    private EditText unameField;
    private EditText passwordField;
    private Button loginButton;
    private CheckBox stayLoggedIn;

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

        loginButton.setOnClickListener(this);

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
        }
    }

    //Not sure what to do here
    public void login(String uname, String pass){
        //Misc Behavior that we will figure out later
    }

    private class LoginProtocol extends AsyncTask<String,Integer,String>{
        @Override
        protected void onPreExecute(){
            //Add a new view to show when it's loading hte grades?
        }
        @Override
        protected String doInBackground(String... params) {
            //We really need to figure out how to pull grades from aspen...
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            //Idk man do something here
        }
    }
}
