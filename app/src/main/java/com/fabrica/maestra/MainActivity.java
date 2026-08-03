package com.fabrica.maestra;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL); lay.setPadding(30,30,30,30);
    TextView t=new TextView(this); t.setText("FABRICA MAESTRA V12 OK"); t.setTextSize(22);
    EditText e=new EditText(this); e.setHint("Token ghp_...");
    lay.addView(t); lay.addView(e);
    setContentView(lay);
  }
}
