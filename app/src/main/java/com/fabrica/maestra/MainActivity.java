package com.fabrica.maestra;
import android.app.Activity; import android.os.Bundle; import android.widget.*;
public class MainActivity extends Activity {
  protected void onCreate(Bundle b){ super.onCreate(b); TextView t=new TextView(this); t.setText("FABRICA V13 OK - COMPILA"); t.setTextSize(24); setContentView(t); }
}
