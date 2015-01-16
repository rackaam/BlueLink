package eu.rakam.bluelink;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.LinkedList;
import java.util.List;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;


public class MainActivity extends ActionBarActivity {

    private BlueLink blueLink;
    private List<String> logs = new LinkedList<>();
    private ArrayAdapter<String> logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blueLink = new BlueLink(this, "BlueLinkTest", "234eda5e-048e-4e75-8acc-b56b6e6cc9aa");
        Button searchForServersButton = (Button) findViewById(R.id.searchForServersButton);
        Button startServerButton = (Button) findViewById(R.id.startServerButton);
        ListView serverListView = (ListView) findViewById(R.id.serverListView);
        ListView logListView = (ListView) findViewById(R.id.logListView);
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        logListView.setAdapter(logAdapter);

        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blueLink.openServer(new OnOpenServerCallback() {
                    @Override
                    public void onFinished(Exception e) {
                        if (e == null)
                            log("Server ON");
                        else
                            log("Error during server initialisation : " + e);
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        blueLink.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void log(String log) {
        logs.add(log);
        logAdapter.notifyDataSetChanged();
    }
}
