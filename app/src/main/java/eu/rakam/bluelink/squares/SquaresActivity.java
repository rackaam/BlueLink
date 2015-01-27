package eu.rakam.bluelink.squares;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import eu.rakam.bluelink.R;
import eu.rakam.bluelinklib.BlueLinkClient;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.Server;
import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;


public class SquaresActivity extends ActionBarActivity {

    private static final String TAG = "BlueLink SquaresActivity";

    private BlueLinkClient blueLinkClient;
    private BlueLinkServer blueLinkServer;
    private List<Server> servers = new LinkedList<>();
    private ArrayAdapter<Server> serverAdapter;
    private View UILayout;
    private GridSurfaceView surfaceView;
    private Model model = new Model();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squares);
        UILayout = findViewById(R.id.UILayout);
        surfaceView = (GridSurfaceView) findViewById(R.id.surfaceView);
        final Button searchForServersButton = (Button) findViewById(R.id.searchForServersButton);
        final Button startServerButton = (Button) findViewById(R.id.startServerButton);
        ListView serverListView = (ListView) findViewById(R.id.serverListView);

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
                        if (e != null) {
                            Log.d(TAG, "Connection error : " + e);
                        } else {
                            Log.d(TAG, "Connected to " + server.getName());
                            startTestClient();
                        }
                    }
                });
            }
        });

        searchForServersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServerButton.setEnabled(false);
                blueLinkClient = new BlueLinkClient(SquaresActivity.this, new Handler(), "BlueLinkTest",
                        "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", new Factory(model), null);
                blueLinkClient.searchForServer(new OnSearchForServerCallback() {
                    @Override
                    public void onSearchStarted() {
                        Log.d(TAG, "Search Started");
                    }

                    @Override
                    public void onNewServer(Server server) {
                        Log.d(TAG, "On New Server : " + server.getName());
                        servers.add(server);
                        serverAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onSearchFinished(List<Server> servers) {
                        Log.d(TAG, "Search Finished");
                        startServerButton.setEnabled(true);
                    }
                });
            }
        });

        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForServersButton.setEnabled(false);
                blueLinkServer = new BlueLinkServer(SquaresActivity.this, new Handler(), "BlueLinkTest",
                        "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", null);
                blueLinkServer.openServer(new OnOpenServerCallback() {
                    @Override
                    public void onOpen(Exception e) {
                        if (e == null)
                            Log.d(TAG, "Server ON");
                        else
                            Log.d(TAG, "Error during server initialisation : " + e);
                    }

                    @Override
                    public void onNewClient(Client client, BlueLinkInputStream in) {
                        String clientName = in.readString();
                        Log.d(TAG, "New client : " + clientName);
                        startTestServer();
                    }
                });
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

}
