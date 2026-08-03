
package com.sene.fabrica;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.util.Base64;
import java.io.*;
import okhttp3.*;
public class MainActivity extends Activity {
  EditText etToken, etRepoName;
  TextView tvLog;
  Uri zipUri=null;
  OkHttpClient client=new OkHttpClient();
  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40,40,40,40);
    TextView title=new TextView(this); title.setText("SENE - Fabrica Maestra"); title.setTextSize(22); title.setTextColor(0xFFA0233D);
    TextView sub=new TextView(this); sub.setText("Soluciones en Electronica");
    etToken=new EditText(this); etToken.setHint("Token ghp_...");
    etRepoName=new EditText(this); etRepoName.setHint("Nombre proyecto");
    Button btnPick=new Button(this); btnPick.setText("1. Seleccionar ZIP");
    Button btnBuild=new Button(this); btnBuild.setText("2. CREAR Y COMPILAR APK"); btnBuild.setBackgroundColor(0xFFA0233D); btnBuild.setTextColor(0xFFFFFFFF);
    tvLog=new TextView(this); tvLog.setText("Log:\nListo.");
    ScrollView sv=new ScrollView(this); sv.addView(tvLog);
    root.addView(title); root.addView(sub); root.addView(etToken); root.addView(etRepoName); root.addView(btnPick); root.addView(btnBuild); root.addView(sv);
    setContentView(root);
    btnPick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("application/zip"); startActivityForResult(i,1001); });
    btnBuild.setOnClickListener(v->{ if(zipUri==null){ log("Selecciona ZIP"); return; } String t=etToken.getText().toString().trim(); String r=etRepoName.getText().toString().trim(); if(t.isEmpty()||r.isEmpty()){ log("Falta token/repo"); return; } new Thread(()->{ try{ crear(t,"1JAVS1",r); }catch(Exception e){ log("Error: "+e.getMessage()); } }).start(); });
  }
  void log(String s){ runOnUiThread(()-> tvLog.append("\n"+s)); }
  @Override protected void onActivityResult(int rc,int res,Intent data){ super.onActivityResult(rc,res,data); if(rc==1001&&data!=null){ zipUri=data.getData(); log("ZIP: "+zipUri); } }
  void crear(String token,String owner,String repo) throws Exception {
    log("Creando repo "+repo);
    String json="{\"name\":\""+repo+"\",\"private\":false}"; Request req=new Request.Builder().url("https://api.github.com/user/repos").header("Authorization","Bearer "+token).post(RequestBody.create(json,MediaType.parse("application/json"))).build(); Response r=client.newCall(req).execute(); log("Repo: "+r.code());
    InputStream is=getContentResolver().openInputStream(zipUri); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=is.read(buf))!=-1) baos.write(buf,0,n); String b64=Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    putFile(token,owner,repo,"project.zip.b64",b64,"ZIP SENE");
    String wf="name: Compilar APK SENE\non: [workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: 8.5\n      - name: Preparar\n        run: |\n          base64 -d project.zip.b64 > project.zip\n          unzip -o project.zip -d .\n          rm project.zip.b64 project.zip\n      - name: Compilar\n        run: gradle assembleDebug --stacktrace\n      - name: Release\n        uses: softprops/action-gh-release@v1\n        with:\n          tag_name: sene-v${{ github.run_number }}\n          files: app/build/outputs/apk/debug/app-debug.apk\n        env:\n          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n";
    putFile(token,owner,repo,".github/workflows/compilar.yml",Base64.encodeToString(wf.getBytes(), Base64.NO_WRAP),"Workflow SENE");
    Request disp=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/compilar.yml/dispatches").header("Authorization","Bearer "+token).post(RequestBody.create("{\"ref\":\"main\"}",MediaType.parse("application/json"))).build(); client.newCall(disp).execute();
    log("Compilacion iniciada https://github.com/"+owner+"/"+repo+"/actions");
  }
  void putFile(String token,String owner,String repo,String path,String b64,String msg) throws Exception {
    String url="https://api.github.com/repos/"+owner+"/"+repo+"/contents/"+path;
    Request get=new Request.Builder().url(url).header("Authorization","Bearer "+token).get().build(); Response rg=client.newCall(get).execute(); String sha=null; if(rg.isSuccessful()){ String body=rg.body().string(); int i=body.indexOf("\"sha\":\""); if(i!=-1){ int s=i+7; int e=body.indexOf("\"",s); sha=body.substring(s,e); } }
    String json="{"+(sha!=null?"\"sha\":\""+sha+"\",":"")+"\"message\":\""+msg+"\",\"content\":\""+b64+"\",\"branch\":\"main\"}";
    Request put=new Request.Builder().url(url).header("Authorization","Bearer "+token).put(RequestBody.create(json,MediaType.parse("application/json"))).build(); Response rp=client.newCall(put).execute(); log(path+" -> "+rp.code());
  }
}
