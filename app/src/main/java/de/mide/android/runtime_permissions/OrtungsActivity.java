package de.mide.android.runtime_permissions;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;


/**
 * Activity, mit der anhand der von GPS-Ortung die Entfernung zu
 * Karlsruhe berechnet werden kann. Hierzu muss die App über die
 * Berechtigung <i>android.permission.ACCESS_FINE_LOCATION</i>
 * verfügen, die (wie die Berechtigung zum Absetzen von Telefon-Anrufen)
 * das <i>Protection Level</i> "Dangerous" hat.<br><br>
 *
 * Geografische Begriffe:
 * <ul>
 *     <li>Geografische Breite (Latitude) : Nördlich oder südlich vom Äquator.</li>
 *     <li>Geografische Länge  (Longitude): Östlich oder westlich von Greenwich.</li>
 *     <li>Bei Koordinaten wird zuerst die Breite und dann die Länge genannt.</li>
 * </ul>
 * <br><br>
 *
 * Beispiele für Dezimal-Koordinaten: siehe Excel-Datei <i>GeografischeKoordinaten.xls</i>
 * im Wurzel-Verzeichnis des App-Projekts.
 * <br><br>
 *
 * This project is licensed under the terms of the BSD 3-Clause License.
 */
@TargetApi(25)
public class OrtungsActivity extends Activity
        implements LocationListener {

    /** Tag für Log-Messages von dieser Activity-Klasse. */
    protected static final String TAG4LOGGING = "OrtungsActivity";

    /** Manager-Objekt zum Zugriff auf Location-API. */
    protected LocationManager _locationManager = null;


    /** Objekt, dass die Koordinaten von Karlsruhe repräsentiert.
     *  Koordinaten von KA laut <a href="https://de.wikipedia.org/wiki/Karlsruhe">Wikipedia</a>:
     *  <ul>
     *      <li><i>49° 1' Nord, 8° 24' Ost</i>.</li>
     *      <li>Als Dezimal-Koordinaten:
     *          <i>+49.014° (Nördl. Breite), +8.4043° (Östl. Länge)</i>.<br>
     *      </li>
     *  </ul>
     *  Bei Dezimal-Koordinaten werden östlich bzw. nördlich durch positive Vorzeichen repräsentiert.
     *  <br>
     *  Das Objekt wird in der Methode {@link OrtungsActivity#onCreate(Bundle)}} erzeugt.
     */
    protected Location _karlsruheLocation = null;

    /** Referenz auf Button <i>berechnungs_button</i> in Layout-Datei. */
    protected Button _buttonEntfernungBerechnen = null;

    /** Referenz auf TextView-Element zur Anzeige des Ergebnis. */
    protected TextView _textviewErgebnis = null;


    /**
     * Lifecycle-Methode, lädt Layout-Datei und erzeugt Location-Objekt mit
     * Koordinaten von Karlsruhe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ortung);

        _textviewErgebnis = findViewById(R.id.ortungsergebnis_textview);

        _buttonEntfernungBerechnen = findViewById(R.id.berechnungs_button);
        // Referenz auf Button-Objekt wird benötigt, um diesen Button während
        // eines laufenden Ortung-Requests zu deaktivieren.

        _karlsruheLocation = new Location("DummyProvider");
        _karlsruheLocation.setLongitude( 8.4043 ); // geografische Länge (West/Ost)
        _karlsruheLocation.setLatitude ( 49.014 ); // geografische Breite (Nord/Süd)
    }


    /**
     * Event-Handler-Methode für Button, um Ortung und anschließende Entfernungs-Berechnung
     * zu starten. Prüft zunächst, ob die App die notwendige Berechtigung schon hat.
     * Wenn ja, dann wird der LocationManager initialisiert und die eigentliche Ortungs-Anfrage
     * gestartet.
     *
     * @param view Button, der das Event ausgelöst hat.
     */
    public void onBerechneEntfernungsButton(View view) {

        _buttonEntfernungBerechnen.setEnabled(false);
        _textviewErgebnis.setText("");


        // Wenn die App auf einem Gerät mit einem kleineren API-Level als 23 läuft,
        // dann wurde die Berechtigung bei der Installation gewährt.
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        if (apiLevel < 23) {

            Log.i(TAG4LOGGING, "API-Level von Gerät < 23, also müssen keine Runtime-Permissions überprüft werden.");
            ortungAnfordern();
            return;
        }

        // wir benötigen mindestens die Berechtigung für ACCESS_COARSE_LOCATION
        if ( checkSelfPermission( Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {

            // App hat schon die Permission
            ortungAnfordern();

        } else {

            String[] permissionArray = { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION };
            requestPermissions( permissionArray, 321 ); // 321: RequestCode (um Callback zuordnen zu können)
            // Callback-Methode: onRequestPermissionsResult
        }
    }


    /**
     * Callback-Methode, wird aufgerufen, wenn nach Aufruf von {@link Activity#requestPermissions(String[], int)}
     * der Nutzer dem System mitgeteilt hat, ob die App die angeforderte Permission bekommt oder nicht.
     *
     * @param requestCode  Ist immer <i>321</i>, da wir in dieser Activity nur eine Permission anfordern.
     *
     * @param permissions  Array mit den Permissions, die wir angefordert haben; enthält für unseren Fall
     *                     immer nur einen Eintrag für die Permission <i>ACCESS_FINE_LOCATION</i>.
     */
    @Override
    public void onRequestPermissionsResult(int      requestCode,
                                           String[] permissions,
                                           int[]    grantResults) {

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            ortungAnfordern();

        } else {

            zeigeFehlerDialog(
                    "Keine Berechtigung zum Abfragen der Ortung erteilt, " +
                            "die Entfernungs-Berechnung kann deshalb nicht durchgeführt werden.");
        }
    }


    /**
     * Methode zum Starten von (asynchronem) Abfragen der Ortung.
     * <br><br>
     *
     * <b>Bevor diese Methode aufgerufen wird muss sichergestellt sein, dass die App die
     * Berechtigung <i>android.permission.ACCESS_FINE_LOCATION</i> hat!</b>
     */
    protected void ortungAnfordern() {

        // Erst noch ggf. LocationManager-Objekt holen
        if (_locationManager == null) {

            boolean locationManagerOkay = holeLocationManager();
            if (!locationManagerOkay) return;
        }

        try {

            _locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            // Methode steht erst ab API-Level 9 zur Verfügung;
            // letztes Argument looper=null (hiermit kann Thread angegeben werden, in dem Call-Back
            // ausgeführt werden soll)
            // Callback-Methode: onLocationChanged
            // liefert Ortung mit Genauigkeit "fine" oder "coarse" zurück

            Log.i(TAG4LOGGING, "Methode requestSingleUpdate() aufgerufen.");

        } catch (SecurityException ex) {

            // Diese Exception sollte eigentlich nie auftreten, weil wir die Methode ortungAnfordern()
            // nur dann aufrufen, wenn überprüft wurde, dass die App aktuell die Berechtigung
            // ACCESS_FINE_LOCATION hat.
            String fehlermeldung = "SecurityException beim Aufruf der requestSingleUpdate()-Methode: " + ex;
            zeigeFehlerDialog(fehlermeldung);
            Log.e(TAG4LOGGING, fehlermeldung);
        }
    }


    /**
     * Initialisierung der Location-API, LocationManager-Objekt holen und an Member-Variable.
     *
     * @return <i>true</i> wenn die Location-API erfolgreich initialisiert werden konnte.
     */
    protected boolean holeLocationManager() {

        String fehlerNachricht = "";

        // LocationManager holen
        _locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (_locationManager == null) {

            fehlerNachricht = "LocationManager-Objekt konnte nicht geholt werden.";
            Log.e(TAG4LOGGING, fehlerNachricht);
            zeigeFehlerDialog(fehlerNachricht);
            return false;

        } else {

            // Alle LocationProvider abfragen und auf Logger ausgeben
            List<String> providerListe = _locationManager.getProviders(true);

            StringBuffer sb = new StringBuffer();
            sb.append("Es wurden ").append(providerListe.size()).append(" LocationProvider gefunden: ");
            for (String providerStr : providerListe) {

                sb.append(providerStr).append(" ");
            }
            sb.append(".");
            Log.i(TAG4LOGGING, sb.toString());


            // GPS-Provider holen
            LocationProvider locationProvider = _locationManager.getProvider(LocationManager.GPS_PROVIDER);
            if (locationProvider == null) {

                fehlerNachricht = "LocationProvider für GPS konnte nicht geholt werden.";
                Log.e(TAG4LOGGING, fehlerNachricht);
                zeigeFehlerDialog(fehlerNachricht);
                return false;

            } else {

                Log.i(TAG4LOGGING, "Location-Manager für GPS konnte geholt werden: " + locationProvider);
                return true;
            }

        } // _locationManager != null
    }


    /**
     * Methode aus Interface {@link android.location.LocationListener}.
     * Wird aufgerufen, wenn mit
     * {@link LocationManager#requestLocationUpdates(long, float, Criteria, LocationListener, Looper)}
     * angeforderte Ortung zur Verfügung steht (dauert also ggf., bis eine neue Berechnung durchgeführt wurde).
     *
     * @param location Ortungs-Objekt
     */
    @Override
    public void onLocationChanged(Location location) {

        Log.i(TAG4LOGGING, "Neue GPS-Ortung erhalten: " + location          );
        Log.i(TAG4LOGGING, "Koordinaten von KA:       " + _karlsruheLocation);

        _textviewErgebnis.append("Aktuelle GPS-Koordinaten:\n\n");
        _textviewErgebnis.append("Breite (N/S): " + location.getLatitude () + "°\n");
        _textviewErgebnis.append("Länge (W/O):  " + location.getLongitude() + "°\n");

        int distanzMeter = (int) location.distanceTo(_karlsruheLocation);
        int distanzKM    = distanzMeter / 1000;

        _textviewErgebnis.append("\nEntfernung zu KA:\n" + distanzKM + " km");

        _buttonEntfernungBerechnen.setEnabled(true);
    }


    /**
     * Methode aus Interface {@link android.location.LocationListener},
     * wird für diese App nicht benötigt.
     *
     * @param provider Name des Ortungs-Providers.
     *
     * @param status Neuer Status des Ortungs-Providers.
     *
     * @param extras Zusätzliche Key-Value-Paare.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        String nachricht = "LocationProvider \"" + provider +
                "\" hat Status gewechselt auf \"" + status + "\".";

        Log.i(TAG4LOGGING, nachricht);
    }


    /**
     * Methode aus Interface {@link android.location.LocationListener},
     * wird für diese App nicht benötigt.
     *
     * @param provider Name des Ortungs-Providers.
     */
    @Override
    public void onProviderEnabled(String provider) {

        String nachricht = "Provider \"" + provider + "\" wurde gerade eingeschaltet.";
        Log.i(TAG4LOGGING, nachricht);
    }


    /**
     * Methode aus Interface {@link android.location.LocationListener},
     * wird für diese App nicht benötigt.
     *
     * @param provider Name des Ortungs-Providers.
     */
    @Override
    public void onProviderDisabled(String provider) {

        String nachricht = "Provider \"" + provider + "\" wurde gerade abgeschaltet.";
        Log.i(TAG4LOGGING, nachricht);
    }


    /**
     * Methode um Fehlermeldung in Dialog anzuzeigen.
     *
     * @param fehlerNachricht Anzuzeigende Fehlermeldung.
     */
    protected void zeigeFehlerDialog(String fehlerNachricht) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Fehlermeldung");
        dialogBuilder.setMessage(fehlerNachricht);
        dialogBuilder.setPositiveButton("Ok", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        _buttonEntfernungBerechnen.setEnabled(true);
    }

}
