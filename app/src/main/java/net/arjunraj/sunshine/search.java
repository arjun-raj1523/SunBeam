package net.arjunraj.sunshine;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class search extends ActionBarActivity {
    String newLocation=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        onPressSearchButton();
    }
    public void onPressSearchButton(){
        Button button_search = (Button) findViewById(R.id.button_search);

        button_search.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                EditText editText_search = (EditText) findViewById(R.id.editText_search);
                newLocation = editText_search.getText().toString();
                Intent i = new Intent(search.this, MainActivity.class);
                if (!newLocation.equals("")) {
                    i.putExtra("newLocation", newLocation);
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
                else{
                    startActivity(i);
                    finish();
                }
            }
        });


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
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
}
