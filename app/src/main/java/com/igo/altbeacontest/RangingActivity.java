package com.igo.altbeacontest;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.w3c.dom.Text;


import java.util.Collection;

public class RangingActivity extends AppCompatActivity implements BeaconConsumer,RangeNotifier {
    private BeaconManager mBeaconManager;
    private TextView tv1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);


        tv1 = (TextView) findViewById(R.id.textView1);

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect the main identifier (UID) frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry (TLM) frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        // Detect the URL frame:
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));

    }

    @Override
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

    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        int count = 0;
        for (final Beacon beacon: beacons) {
            count+=1;
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {

                // This is a Eddystone-UID frame
                double RSSI = beacon.getRssi();
                final Identifier namespaceId = beacon.getId1();
                final Identifier instanceId = beacon.getId2();

                /*Log.d("Ranging", "I see a beacon transmitting namespace id: "+namespaceId+
                        " and instance id: "+instanceId+
                        " approximately "+beacon.getDistance()+" meters away.");*/

                String TextoTele = "";

                // Do we have telemetry data?
                if (beacon.getExtraDataFields().size() > 0) {
                    long telemetryVersion = beacon.getExtraDataFields().get(0);
                    long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                    long pduCount = beacon.getExtraDataFields().get(3);
                    long uptime = beacon.getExtraDataFields().get(4);

                    long unsignedTemp = (beacon.getExtraDataFields().get(2) >> 8);
                    double temperature = unsignedTemp > 128 ?
                            unsignedTemp - 256 :
                            unsignedTemp +(beacon.getExtraDataFields().get(2) & 0xff)/256.0;

                    /*Log.d("Ranging", "The above beacon is sending telemetry version "+telemetryVersion+
                            ", temperatura de : " + temperature +" °C" +
                            ", has been up for : "+uptime+" seconds"+
                            ", has a battery level of "+batteryMilliVolts+" mV"+
                            ", and has transmitted "+pduCount+" advertisements.");*/

                    TextoTele =  "\ntemperatura : " + temperature +" °C";

                }
                final String TextoBeacon = "namespace id: "+namespaceId+ "\ninstance id: "+instanceId+ "\ndistancia: "
                        + String.format("%.3f", beacon.getDistance()) + TextoTele + "\nRSSI: " + RSSI;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv1.setText(TextoBeacon);

                    }
                });

            }
            Log.e("Conteo", String.valueOf(count));
        }

        Log.e("Fin", "Fin del bucle");

    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }

}
