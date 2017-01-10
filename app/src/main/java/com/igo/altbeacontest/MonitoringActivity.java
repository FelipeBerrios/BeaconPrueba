package com.igo.altbeacontest;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity implements BeaconConsumer,  RangeNotifier {
    protected static final String TAG = "MonitoringActivity";
    private BeaconManager mBeaconManager;
    private CardView cv;
    private Chronometer cn;
    private TextView tvRegion ;
    private TextView tvTextR ;
    private ImageView imageview1;
    Map<String, String> regionID = new HashMap<String, String>();
    Map<String, String> imagenID = new HashMap<String, String>();
    Map<String, Long> timestampsLast = new HashMap<String, Long>();
    Map<String, Double> distanceLast = new HashMap<String, Double>();
    Map<String, Long> timestampsLastCopy = new HashMap<String, Long>();
    Map<String, Double> distanceLastCopy = new HashMap<String, Double>();
    private String MasCercano ="";
    private Double distanciaCercana = Double.POSITIVE_INFINITY;
    private int elapsedTime=0;
    private boolean isRunning =false;
    private long timestamp = 0;
    private long comienzo;
    private String antiguo="";
    private String mejor="";
    private ProgressBar pb;
    private TextView tt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
// Detect the main identifier (UID) frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
// Detect the telemetry (TLM) frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
// Detect the URL frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));

        pb = (ProgressBar) findViewById(R.id.progreso_inicial);

        cn = (Chronometer) findViewById(R.id.cm1);
        cn.setVisibility(View.INVISIBLE);

        tt = (TextView) findViewById(R.id.tt);
        tt.setVisibility(View.INVISIBLE);

        tvRegion = (TextView) findViewById(R.id.region);
        tvRegion.setVisibility(View.INVISIBLE);

        tvTextR = (TextView) findViewById(R.id.textr);
        tvTextR.setVisibility(View.INVISIBLE);

        imageview1 = (ImageView) findViewById(R.id.imagen);

        regionID.put("0x464279723052","Desarrollo");
        regionID.put("0x31346631486c","Oficina");
        imagenID.put("0x464279723052","desarrollo");
        imagenID.put("0x31346631486c","oficina");

        mBeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(20000l);
        mBeaconManager.setForegroundScanPeriod(1100l);
        comienzo = SystemClock.elapsedRealtime();
    }

    public void onResume() {
        super.onResume();
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main Eddystone-UID frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry Eddystone-TLM frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        mBeaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        // Set the two identifiers below to null to detect any beacon regardless of identifiers
        Identifier myBeaconNamespaceId = Identifier.parse("0xf7826da6bc5b71e0893e");
        Identifier myBeaconInstanceId = Identifier.parse("0x31346631486c");
        Region region = new Region("my-beacon-region", Identifier.parse("0xf7826da6bc5b71e0893e"), null, null);
        /*mBeaconManager.addMonitorNotifier(this);
        try {
            mBeaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }*/
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
    }

    /*public void didEnterRegion(Region region) {
        Log.d(TAG, "I detected a beacon in the region with namespace id " + region.getId1() +
                " and instance id: " + region.getId2());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cn.start();

            }
        });

    }

    public void didExitRegion(Region region) {
        Log.i(TAG, "I no longer see an beacon");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cn.stop();

            }
        });
    }

    public void didDetermineStateForRegion(int state, Region region) {
        Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
    }*/

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {


        distanciaCercana = Double.POSITIVE_INFINITY;
        String nuevoCercano = MasCercano;
        if(beacons.size()==0){
            return;
        }

        for (final Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {

                // This is a Eddystone-UID frame
                double RSSI = beacon.getRssi();
                final Identifier namespaceId = beacon.getId1();
                final Identifier instanceId = beacon.getId2();
                final double distActual = beacon.getDistance();

                timestampsLast.put(instanceId.toString(), SystemClock.elapsedRealtime());
                distanceLast.put(instanceId.toString(), distActual);

                Log.e("Valores: ", "InstanceID: " + instanceId + " Distancia: " + distActual);

                Log.e("HashMap",distanceLast.toString());

                /*Log.d("Ranging", "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " approximately "+beacon.getDistance()+" meters away.");*/



                //String TextoTele = "";

                /*if(distActual <= distanciaCercana ){
                    nuevoCercano = instanceId.toString();
                    distanciaCercana = distActual;
                }*/
                /*final String TextoBeacon = "namespace id: "+namespaceId+ "\ninstance id: "+instanceId+ "\ndistancia: "
                        + String.format("%.3f", beacon.getDistance()) + TextoTele + "\nRSSI: " + RSSI;*/
            }

        }

        Log.e("Hola", "---------------------------Fin-----------------------------------");
        //timestampsLastCopy.putAll(timestampsLast);
        //distanceLastCopy.putAll(distanceLast);
        Log.e("HashMap2",distanceLast.toString());

        distanciaCercana = Double.POSITIVE_INFINITY;

        Iterator it = timestampsLast.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(SystemClock.elapsedRealtime() - (long) pair.getValue() > 10000){
                Log.e("Removiendo", "Removiendoooooooooo");
                it.remove();
                distanceLast.remove(pair.getKey());
            }

        }

        it = distanceLast.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if((double) pair.getValue() < distanciaCercana){
                distanciaCercana = (double) pair.getValue();
                mejor = (String) pair.getKey();
            }
        }
        /*if(!ultimoVisto){
            return;
        }*/

        if(isRunning){
            if(!mejor.equals(antiguo)){
                Log.e("Distinto", "Entre distinto");
                timestamp = SystemClock.elapsedRealtime() - cn.getBase();
                final int seconds = (int) (timestamp / 1000) % 60 ;
                final int minutes = (int) ((timestamp / (1000*60)) % 60);
                final int hours   = (int) ((timestamp / (1000*60*60)) % 24);
                if(timestamp > 2000){
                    Log.e("MEJOR: ", mejor);
                    Log.e("ANTIGUO: ", antiguo);
                    final int resID = getResources().getIdentifier(imagenID.get(mejor) , "mipmap", getPackageName());
                    final String auxiliar_antiguo = antiguo;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("Cambia Interfaz", "CAMBIO INTERFAZ");
                            imageview1.setImageResource(resID);
                            tvRegion.setText(regionID.get(mejor));
                            tvTextR.setText("Ultima Region Visitada: " + regionID.get(auxiliar_antiguo)
                                    + "\nTiempo Aproximado: " + String.format("%02d:%02d:%02d", hours,minutes,seconds));
                            cn.stop();
                            cn.setBase(SystemClock.elapsedRealtime());
                            cn.start();
                        }
                    });
                    antiguo = mejor;
                }
            }
            else{
                antiguo = mejor;
            }

        }
        else{
            if(SystemClock.elapsedRealtime() - comienzo >5000){
                antiguo = mejor;
                final int resID = getResources().getIdentifier(imagenID.get(mejor) , "mipmap", getPackageName());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvRegion.setVisibility(View.VISIBLE);
                        tvTextR.setVisibility(View.VISIBLE);
                        cn.setVisibility(View.VISIBLE);
                        pb.setVisibility(View.GONE);
                        tt.setVisibility(View.VISIBLE);

                        imageview1.setImageResource(resID);
                        tvRegion.setText(regionID.get(mejor));
                        tvTextR.setText("Sin informacion Anterior");
                        cn.setBase(SystemClock.elapsedRealtime());
                        cn.start();
                    }
                });
                isRunning = true;

            }

        }
        //timestampsLast.putAll(timestampsLastCopy) ;
        //distanceLast.putAll(distanceLastCopy) ;
        Log.e("HashMap3",distanceLast.toString());


    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }


}
