package de.mide.runtimepermissions;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


/**
 * Demo für "Runtime Permissions" (neu ab Android 6.0 "Marshmallow", API-Level 23).
 * <br><br>
 *
 * Siehe auch:
 * <ul>
 *   <li><a href="http://developer.android.com/preview/features/runtime-permissions.html">Doku zum neuen Permission-Modell</a>.</li>
 *   <li><a href="http://developer.android.com/reference/android/Manifest.permission.html">API-Doku-Seite</a> mit Permissions-Liste, auf
 *      der für die einzelnen Permissions angegeben ist, ob das <i>Protection Level</i> entweder <i>Dangerous</i> oder <i>normal</i> ist.</li>
 *   <li><a href="android-developers.blogspot.com/2015/08/building-better-apps-with-runtime.html">Artikel auf dem <i>Android Developers Blog</i></a>.</li>
 * </ul>
 * <br><br>
 *
 * Diese Klasse wird mit <i>@TargetApi(25)</i> annotiert, weil das Minimum-API-Level der App 8 ist, aber
 * für die Runtime Permissions Methoden im Quelltext auftauchen (nämlich {@link Activity#requestPermissions(String[], int)}
 * und {@link Activity#checkSelfPermission(String)}), die erst mit API-Level 23 eingeführt wurden.
 * Diese Methoden werden aber nur dann aufgerufen, wenn zur Laufzeit durch Auslesen von
 * <i>android.os.Build.VERSION.SDK_INT</i> festgestellt wurde, dass die App auf einem Gerät mit mindestens
 * API-Level 23 läuft.
 * <a href="https://developer.android.com/reference/android/annotation/TargetApi.html">API-Doc zu Annotation TargetAPI</a>.
 * <br><br>
 *
 * Die Zuweisung der Event-Handler für die beiden Buttons geschieht über das Attribut <i>android:onClick</i> in den jeweiligen
 * Elementen der Layout-Datei.
 * <br><br>
 *
 * This project is licensed under the terms of the BSD 3-Clause License.
 */
@TargetApi(25)
public class MainActivity extends Activity {

    /** Tag für Log-Messages von dieser Activity-Klasse. */
    protected static final String TAG4LOGGING = "RuntimePermissions";


    /**
     * Lifecycle-Methode zur Initialisierung des Activity-Objekts.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    /**
     * Event-Handler für den Button zum Abfragen des WLAN-Status.
     * <br><br>
     *
     * Für das Abfragen des WLAN-Status muss die Permission
     * <a href="http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_WIFI_STATE">android.permission.ACCESS_WIFI_STATE</a>
     * in der Manifest-Datei eingetragen sein. Es handelt sich hierbei um eine Permission, die das
     * <i>Protection Level: Normal</i> hat. Diese Permissions werden bei der Installation wie schon beim
     * "alten" Permission-Modell vor Android 6.0 behandelt.
     *
     * @param view Referenz auf das Button-Element, das dieses Event ausgelöst hat.
     */
    public void onWLANStatusButton(View view) {

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(this, "WifiManager nicht verfügbar", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "WLAN ist eingeschaltet: " + wifiManager.isWifiEnabled(),
                Toast.LENGTH_LONG).show();
    }


    /**
     * Event-Handler für den Button um eine Telefon-Nummer anzuwählen (über einen impliziten Intent).
     * <br><br>
     *
     * Für das Anwählen einer Telefon-Nummer muss die Permission
     * <a href="http://developer.android.com/reference/android/Manifest.permission.html#CALL_PHONE">android.permission.CALL_PHONE</a>
     * in der Manifest-Datei eingetragen sein. Es handelt sich hierbei um eine Permission mit <i>Protection level: Dangerous</i>,
     * deshalb muss bei Geräten mit Android ab Version 6.0 zur Laufzeit die Permission angefordert/überprüft werden.
     *
     * @param view Referenz auf das UI-Element, das dieses Event ausgelöst hat.
     */
    public void onPhoneCallButton(View view) {

        // API-Level der Android-Version auf dem aktuellen Gerät abfragen
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        if (apiLevel < 23) {
            telefonnummerAnfrufen();
            return;
        }

        // Achtung: die Methode "checkSelfPermission" gibt es erst ab API-Level 23.
        // Das folgende Coding darf also nicht ausgeführt werden, wenn diese App auf einem
        // Gerät mit einer Android-Version vor API-Level 23 ausgeführt wird.
        //
        // Damit kein Fehler für Methode checkSelfPermission() angezeigt wird (weil diese ab
        // API-Level 23 definiert ist), wurde die Klasse mit "@TargetApi(24)" annotiert.
        if ( checkSelfPermission( Manifest.permission.CALL_PHONE ) == PackageManager.PERMISSION_GRANTED ) {

            telefonnummerAnfrufen();

        } else {

            // Abfragen, ob wir vor der Berechtigungs-Anfrage dem Nutzer noch eine Erklärung anzeigen sollen,
            // warum die App die Berechtigung benötigt. Die Methode shouldShowRequestPermissionRationale()
            // liefert dann "true" zurück, wenn der Nutzer der App die Berechtigung beim letzten Mal
            // verweigert hat.
            /*
            if ( shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE) ) {
                Toast.makeText(this,
                               "Um Anrufe abzusetzen braucht die App die entsprechende Permission.",
                               Toast.LENGTH_LONG).show();
            } */


            // Wenn zweite Permission einkommentiert, dann auch in Manifest-Datei zugehöriges
            // "uses-permission"-Element einkommentieren. Es kann dann ausprobiert werden,
            // wie der Dialog zur Anforderung von zwei Permissions auf einmal aussieht.
            String[] permissionArray = { Manifest.permission.CALL_PHONE
                                     /*, Manifest.permission.ACCESS_FINE_LOCATION */ };

            requestPermissions( permissionArray, 123 ); // 123: RequestCode (um Callback zuordnen zu können)
            // Auch die Methode requestPermissions() gibt es erst ab API-Level 23, deshalb Klasse
            // mit "@TargetApi(24)" annotiert.
        }
    }


    /**
     * Callback-Methode, wird aufgerufen, wenn nach Aufruf von {@link Activity#requestPermissions(String[], int)}
     * der Nutzer dem System mitgeteilt hat, ob die App die angeforderte Permission bekommt oder nicht.
     *
     * @param requestCode  Ist immer <i>123</i>, da wir in dieser Activity nur eine Permission anfordern.
     *
     * @param permissions  Array mit den Permissions, die wir angefordert haben; enthält für unseren Fall
     *                     immer nur einen Eintrag für die Permission <i>CALL_PHONE</i>.
     *
     * @param grantResults Array mit den "Antworten" auf die Genehmigungsanfragen.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            telefonnummerAnfrufen();

        } else {

            Toast.makeText(this,
                    "Berechtigung verweigert, deshalb kann keine Telefon-Nummer angewählt werden.",
                    Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Methode zum eigentlichen Anwählen einer Telefon-Nummer. Diese Methode darf nur aufgerufen
     * werden, wenn vorher überprüft worden ist, dass die App über die Permission
     * <i>android.permission.CALL_PHONE</i> verfügt.
     */
    protected void telefonnummerAnfrufen() {

        Intent intent = new Intent( Intent.ACTION_CALL, Uri.parse("tel:1234567890") );

        if ( wirdIntentUnterstuetzt(intent) ) {

            try {
                startActivity(intent);
            }
            catch (SecurityException ex) {
                // Diese Exception sollte nicht auftreten, weil wir diese Methode nur dann
                // aufrufen, wenn wird in der Methode onRequestPermissionsResult() festgestellt
                // haben, dass wir die Runtime-Permission haben
                Log.e(TAG4LOGGING, "Exception beim Versenden von Intent aufgetreten." + ex);
            }

        } else {

            Toast.makeText(this, "Keine Telefonie-App vorhanden.", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Methode zum Überprüfen, ob <i>intent</i> auf dem aktuellen Ger#t verarbeitet werden kann
     * (d.h. ob es mindestens eine App installiert ist, die diesen Intent verarbeiten kann).
     *
     * @param intent Impliziter Intent, für den zu prüfen ist, ob er vom Gerät verarbeitet werden kann.
     *
     * @return <i>true</i>, wenn der Intent verarbeitet werden kann, sonst <i>false</i>.
     */
    protected boolean wirdIntentUnterstuetzt(Intent intent) {

        PackageManager packageManager = this.getPackageManager();

        ComponentName componentName = intent.resolveActivity(packageManager);

        if (componentName == null) {
            
            return false;
            
        } else {
            
            return true;
        }
    }


    /**
     * Event-Handler-Methode, um Activity zur Entfernungs-Berechnung zu öffnen.
     *
     * @param view Referenz auf das Button-Element, das dieses Event ausgelöst hat.
     */
    public void onEntfernungKaButton(View view) {

        Intent intent = new Intent(this, OrtungsActivity.class);
        startActivity(intent);
    }

}
