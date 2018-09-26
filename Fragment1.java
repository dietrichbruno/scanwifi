package br.ind.klein.econuvem;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import br.ind.klein.econuvem.Fragment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Fragment1 extends Fragment{

    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private Button buttonJson;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private Context thiscontext;
    private TextView textView;
    myBroadcastReceiver mbroadcastReceiver = new myBroadcastReceiver();
    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;

    IntentFilter intentFilter = new IntentFilter();

    String ssidatual;
    String passatual;

    String txtJson;
    ProgressDialog pd;
    ProgressDialog pd2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.content_wifi, container, false);

        buttonScan = view.findViewById(R.id.scanBtn);
        buttonJson = view.findViewById(R.id.jsonBtn);
        textView = view.findViewById(R.id.textView2);

        buttonScan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                scanWifi();
            }
        });

        listView = view.findViewById(R.id.wifiList);
        wifiManager = (WifiManager) thiscontext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if(!wifiManager.isWifiEnabled()) {
            Toast.makeText(thiscontext, "Wifi is disabled...You need to enable it", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        adapter = new ArrayAdapter<>(thiscontext, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        // CLICANDO NOS ITENS DA LISTVIEW DA WIFI

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Ecopro","Iniciando onclicklistener");
                String posicao = arrayList.get(position);

                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = String.format("\"%s\"", results.get(position).SSID);
                //conf.preSharedKey="\"password\"";
                Log.i("Capabii",results.get(position).capabilities);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); // This is for a public network which dont have any authentication

                //Add the created wifi configuration to device
                int netId = wifiManager.addNetwork(conf);

                wifiManager.disconnect();

                wifiManager.enableNetwork(netId, true);

                thiscontext.registerReceiver(mbroadcastReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

                pd = new ProgressDialog(thiscontext);
                pd.setMessage("Conectando...");
                pd.setCancelable(false);
                pd.show();

                final Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() { wifiManager.reconnect();

                    }
                }, 1000);

            }

        });

        // Pegar JSON
        buttonJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                Log.i("econuvem","entrou no pegar json");
                if (ssidatual.startsWith("\"") && ssidatual.endsWith("\"")){
                    ssidatual = ssidatual.substring(1, ssidatual.length()-1);
                }
				
				//Baixando um arquivo JSON no backend
                new JsonTask().execute("http://192.168.4.1/asyncupdate.json");

            }
        });

        return view;
    }

    private void scanWifi() {

        arrayList.clear();
        thiscontext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

        Toast.makeText(thiscontext, "scanning", Toast.LENGTH_SHORT).show();
    };

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                results = wifiManager.getScanResults();

                thiscontext.unregisterReceiver(this);

                for (ScanResult scanResult : results) {

                    arrayList.add(scanResult.SSID);

                    adapter.notifyDataSetChanged();
                }

            } catch(Exception e) {
                Log.d("Erro do BroadCast", "erro!1!", e);
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        this.thiscontext = context;
        super.onAttach(context);
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd2 = new ProgressDialog(thiscontext);
            pd2.setMessage("Please wait");
            pd2.setCancelable(false);
            pd2.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);

                    //here u ll get whole response...... :-)

                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd2.isShowing()){
                pd2.dismiss();
            }
            txtJson = result;

            textView.setText(txtJson);

        }
    }

    public class myBroadcastReceiver extends BroadcastReceiver {
        public myBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Meu aplicativo","Recebendo");
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null) {
                Log.i("Meu aplicativo", "Type : " + networkInfo.getType()
                        + "State : " + networkInfo.getState());

                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                    //get the different network states
                    //networkInfo.getState() == NetworkInfo.State.CONNECTING
                    if ( networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        if (pd.isShowing()){

                            pd.dismiss();

                        }
                        thiscontext.unregisterReceiver(this);

                        ((MainActivity)getActivity()).setViewPager(1);
                    }
                }
            }

        }
    }

}
