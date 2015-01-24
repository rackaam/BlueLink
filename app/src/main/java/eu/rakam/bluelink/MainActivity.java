package eu.rakam.bluelink;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import eu.rakam.bluelinklib.BlueLinkClient;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.Server;
import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "BlueLinkTest";

    private BlueLinkClient blueLinkClient;
    private BlueLinkServer blueLinkServer;
    private List<String> logs = new LinkedList<>();
    private List<Server> servers = new LinkedList<>();
    private ArrayAdapter<Server> serverAdapter;
    private ArrayAdapter<String> logAdapter;
    private MessageProcessor messageProcessor = new MessageProcessor(this);
    private View UILayout;
    private GridSurfaceView surfaceView;
    private Model model = new Model();
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.messageField);
        UILayout = findViewById(R.id.UILayout);
        surfaceView = (GridSurfaceView) findViewById(R.id.surfaceView);
        Button searchForServersButton = (Button) findViewById(R.id.searchForServersButton);
        Button startServerButton = (Button) findViewById(R.id.startServerButton);
        Button sendButton = (Button) findViewById(R.id.sendButton);

        final ListView serverListView = (ListView) findViewById(R.id.serverListView);
        serverAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, servers);
        serverListView.setAdapter(serverAdapter);
        serverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Server server = servers.get(position);
                BlueLinkOutputStream out = new BlueLinkOutputStream();
                out.writeString(Build.MODEL);
                blueLinkClient.connectToServer(server, out, new OnConnectToServerCallback() {
                    @Override
                    public void onConnect(IOException e) {
                        editText.setText("azerty");
                        if (e != null) {
                            log("Connection error : " + e);
                        } else {
                            log("Connected to " + server.getName());
                            startTestClient();
                        }
                    }
                });
            }
        });

        ListView logListView = (ListView) findViewById(R.id.logListView);
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        logListView.setAdapter(logAdapter);

        searchForServersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blueLinkClient = new BlueLinkClient(MainActivity.this, new Handler(), "BlueLinkTest",
                        "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", new Factory(model), messageProcessor);
                blueLinkClient.searchForServer(new OnSearchForServerCallback() {
                    @Override
                    public void onSearchStarted() {
                        editText.setText("azerty");
                        Log.d(TAG, "Search Started");
                    }

                    @Override
                    public void onNewServer(Server server) {
                        editText.setText("azerty");
                        Log.d(TAG, "New Server : " + server.getName());
                        servers.add(server);
                        serverAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onSearchFinished(List<Server> servers) {
                        editText.setText("azerty");
                        Log.d(TAG, "Search Finished");
                    }
                });
            }
        });

        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blueLinkServer = new BlueLinkServer(MainActivity.this, new Handler(), "BlueLinkTest",
                        "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", messageProcessor);
                blueLinkServer.openServer(new OnOpenServerCallback() {
                    @Override
                    public void onOpen(Exception e) {
                        editText.setText("azerty");
                        if (e == null)
                            log("Server ON");
                        else
                            log("Error during server initialisation : " + e);
                    }

                    @Override
                    public void onNewClient(Client client, BlueLinkInputStream in) {
                        editText.setText("azerty");
                        String clientName = in.readString();
                        log("New client : " + clientName);
                        startTestServer();
                    }
                });
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlueLinkOutputStream message = new BlueLinkOutputStream();
                message.writeString("Test");
                message.writeInt(45678);
                message.writeFloat(54.42f);
                if (blueLinkClient != null)
                    blueLinkClient.sendMessage(message);
                if (blueLinkServer != null)
                    blueLinkServer.broadcastMessage(message);
            }
        });

    }

    private void startTestServer() {
        UILayout.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        Engine engine = new Engine(this, blueLinkServer, model);
        surfaceView.setModel(model);
        new EngineThread(engine).start();
        new RenderingThread(this, surfaceView).start();
    }

    private void startTestClient() {
        UILayout.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setModel(model);
        new RenderingThread(this, surfaceView).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (blueLinkClient != null)
            blueLinkClient.onActivityResult(requestCode, resultCode, data);
        if (blueLinkServer != null)
            blueLinkServer.onActivityResult(requestCode, resultCode, data);
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
