
package com.sene.fabrica;
import android.app.*; import android.content.*; import android.net.Uri; import android.os.*; import android.widget.*; import java.io.*; import java.util.*; import java.util.zip.*; import okhttp3.*; import org.json.*; import android.util.Base64;
public class MainActivity extends Activity{
 EditText etToken,etRepo; TextView tvLog,tvStatus,tvVal; Button btnDown,btnBuild,btnError; Uri zipUri=null; OkHttpClient client=new OkHttpClient(); String apkUrl="",repoUrl=""; String ownerG="",repoG=""; boolean okZip=false;
 @Override protected void onCreate(Bundle b){
  super.onCreate(b);
  LinearLayout r=new LinearLayout(this); r.setOrientation(1); r.setPadding(30,30,30,30); r.setBackgroundColor(0xFFF5F5F5);
  TextView t=new TextView(this); t.setText("SENE FABRICA V23"); t.setTextSize(22); t.setTextColor(0xFFA0233D); t.setTypeface(null,1);
  etToken=new EditText(this); etToken.setHint("Token ghp_... NUEVO, revoca el viejo"); etToken.setBackgroundColor(-1);
  etRepo=new EditText(this); etRepo.setHint("Nombre proyecto"); etRepo.setBackgroundColor(-1);
  Button pick=new Button(this); pick.setText("1. SELECCIONAR ZIP"); pick.setBackgroundColor(0xFF607D8B); pick.setTextColor(-1);
  tvVal=new TextView(this); tvVal.setText("Validador: esperando"); tvVal.setPadding(20,20,20,20); tvVal.setBackgroundColor(0xFFEEEEEE);
  btnBuild=new Button(this); btnBuild.setText("2. VALIDAR Y COMPILAR"); btnBuild.setBackgroundColor(0xFF9E9E9E); btnBuild.setTextColor(-1); btnBuild.setEnabled(false);
  tvStatus=new TextView(this); tvStatus.setText("Estado: esperando"); tvStatus.setPadding(20,20,20,20); tvStatus.setBackgroundColor(0xFFFFFF00);
  btnDown=new Button(this); btnDown.setText("⬇️ DESCARGAR APK"); btnDown.setBackgroundColor(0xFF4CAF50); btnDown.setTextColor(-1); btnDown.setTextSize(18); btnDown.setVisibility(8);
  btnError=new Button(this); btnError.setText("❌ VER ERROR EN GITHUB"); btnError.setBackgroundColor(0xFFF44336); btnError.setTextColor(-1); btnError.setVisibility(8);
  tvLog=new TextView(this); tvLog.setText("Log:");
  ScrollView sv=new ScrollView(this); sv.addView(tvLog);
  r.addView(t); r.addView(etToken); r.addView(etRepo); r.addView(pick); r.addView(tvVal); r.addView(btnBuild); r.addView(tvStatus); r.addView(btnDown); r.addView(btnError); r.addView(sv);
  setContentView(r);
  pick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("*/*"); startActivityForResult(i,1001); });
  btnBuild.setOnClickListener(v->{
   if(!okZip) return;
   String tok=etToken.getText().toString().trim(); String rep=etRepo.getText().toString().trim();
   if(tok.isEmpty()||rep.isEmpty()){ toast("Falta datos"); return; }
   ownerG="1JAVS1"; repoG=rep; repoUrl="https://github.com/"+ownerG+"/"+repoG+"/actions";
   btnDown.setVisibility(8); btnError.setVisibility(8);
   new Thread(()->{ try{ crear(tok,ownerG,rep); }catch(Exception e){ log("Error "+e.getMessage()); setS("Error: "+e.getMessage(),0xFFF44336); } }).start();
  });
  btnDown.setOnClickListener(v->{
   if(apkUrl.isEmpty()) return;
   DownloadManager dm=(DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
   DownloadManager.Request rq=new DownloadManager.Request(Uri.parse(apkUrl));
   rq.setTitle("SENE APK"); rq.setNotificationVisibility(1); rq.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, etRepo.getText().toString().trim()+".apk");
   dm.enqueue(rq); toast("Descargando");
  });
  btnError.setOnClickListener(v->{
   Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl));
   startActivity(i);
  });
 }
 void log(String s){ runOnUiThread(()-> tvLog.append("\n"+s)); }
 void setS(String s,int c){ runOnUiThread(()->{ tvStatus.setText(s); tvStatus.setBackgroundColor(c); }); }
 void toast(String s){ runOnUiThread(()-> Toast.makeText(this,s,1).show()); }
 @Override protected void onActivityResult(int a,int b,Intent d){
  super.onActivityResult(a,b,d);
  if(a==1001&&d!=null){ zipUri=d.getData(); log("ZIP: "+zipUri); new Thread(()-> validar(zipUri)).start(); }
 }
 void validar(Uri uri){
  try{
   InputStream is=getContentResolver().openInputStream(uri); ZipInputStream zis=new ZipInputStream(is); Set<String> set=new HashSet<>(); ZipEntry e; while((e=zis.getNextEntry())!=null){ set.add(e.getName()); zis.closeEntry(); } zis.close();
   boolean a=set.contains("app/build.gradle")||set.stream().anyMatch(s->s.endsWith("app/build.gradle"));
   boolean s=set.contains("settings.gradle")||set.stream().anyMatch(x->x.endsWith("settings.gradle"));
   boolean w=set.stream().anyMatch(x->x.contains("gradle-wrapper.properties"));
   boolean m=set.stream().anyMatch(x->x.contains("AndroidManifest.xml"));
   boolean j=set.stream().anyMatch(x->x.contains("MainActivity.java")||x.contains("MainActivity.kt"));
   StringBuilder sb=new StringBuilder(); sb.append("VALIDACION:\n"); sb.append((a?"[OK] ":"[X] ")+"app/build.gradle\n"); sb.append((s?"[OK] ":"[X] ")+"settings.gradle\n"); sb.append((w?"[OK] ":"[X] ")+"wrapper\n"); sb.append((m?"[OK] ":"[X] ")+"Manifest\n"); sb.append((j?"[OK] ":"[X] ")+"MainActivity\n");
   if(a&&s&&w&&m&&j){ sb.append("\nZIP VALIDO"); okZip=true; runOnUiThread(()->{ btnBuild.setEnabled(true); btnBuild.setBackgroundColor(0xFFA0233D); }); setS("ZIP VALIDO - listo",0xFF8BC34A); }
   else{ sb.append("\nZIP INVALIDO"); okZip=false; runOnUiThread(()->{ btnBuild.setEnabled(false); btnBuild.setBackgroundColor(0xFF9E9E9E); }); setS("ZIP INVALIDO",0xFFF44336); }
   runOnUiThread(()-> tvVal.setText(sb.toString()));
  }catch(Exception ex){ runOnUiThread(()-> tvVal.setText("Error: "+ex.getMessage())); }
 }
 void crear(String token,String owner,String repo) throws Exception{
  setS("Creando repo "+repo,0xFF03A9F4);
  String js="{\"name\":\""+repo+"\",\"private\":false}"; Request rq=new Request.Builder().url("https://api.github.com/user/repos").header("Authorization","Bearer "+token).post(RequestBody.create(js,MediaType.parse("application/json"))).build(); client.newCall(rq).execute();
  setS("Subiendo ZIP",0xFF03A9F4);
  InputStream is=getContentResolver().openInputStream(zipUri); ByteArrayOutputStream ba=new ByteArrayOutputStream(); byte[] bf=new byte[8192]; int n; while((n=is.read(bf))!=-1) ba.write(bf,0,n); String b64=Base64.encodeToString(ba.toByteArray(),0);
  put(token,owner,repo,"project.zip.b64",b64,"zip");
  String wf="name: Compilar APK SENE\non: [workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: 8.5\n      - name: Preparar\n        run: |\n          base64 -d project.zip.b64 > project.zip\n          unzip -o project.zip -d .\n          rm project.zip.b64 project.zip\n      - name: Compilar\n        run: gradle assembleDebug --stacktrace\n      - name: Release\n        uses: softprops/action-gh-release@v1\n        with:\n          tag_name: sene-v${{ github.run_number }}\n          files: app/build/outputs/apk/debug/app-debug.apk\n        env:\n          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n";
  put(token,owner,repo,".github/workflows/compilar.yml",Base64.encodeToString(wf.getBytes(),0),"wf");
  Request disp=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/compilar.yml/dispatches").header("Authorization","Bearer "+token).post(RequestBody.create("{\"ref\":\"main\"}",MediaType.parse("application/json"))).build(); client.newCall(disp).execute();
  setS("Compilando... 0s",0xFFFFFF00);
  for(int i=0;i<15;i++){
   Thread.sleep(20000);
   int sec=(i+1)*20; setS("Compilando "+sec+"s / 300s",0xFFFFFF00);
   try{
    Request ch=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/releases/latest").header("Authorization","Bearer "+token).get().build(); Response cr=client.newCall(ch).execute();
    if(cr.isSuccessful()){
     JSONObject jo=new JSONObject(cr.body().string());
     if(jo.has("assets")){
      org.json.JSONArray ar=jo.getJSONArray("assets");
      if(ar.length()>0){
       apkUrl=ar.getJSONObject(0).getString("browser_download_url");
       setS("¡APK LISTA!",0xFF4CAF50); runOnUiThread(()-> btnDown.setVisibility(0)); log("Lista: "+apkUrl); return;
      }
     }
    }
    Request runReq=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs?per_page=1").header("Authorization","Bearer "+token).get().build(); Response rr=client.newCall(runReq).execute();
    if(rr.isSuccessful()){
     JSONObject jo2=new JSONObject(rr.body().string()); org.json.JSONArray runs=jo2.getJSONArray("workflow_runs");
     if(runs.length()>0){ JSONObject run=runs.getJSONObject(0); String status=run.getString("status"); String concl=run.optString("conclusion",""); log("GitHub: "+status+" "+concl);
      if(concl.equals("failure")){ setS("FALLO - ver error",0xFFF44336); runOnUiThread(()-> btnError.setVisibility(0)); return; }
     }
    }
   }catch(Exception ex){ log("Poll error "+ex.getMessage()); }
  }
  setS("Tiempo excedido 300s - revisa GitHub",0xFFF44336); runOnUiThread(()-> btnError.setVisibility(0));
 }
 void put(String token,String owner,String repo,String path,String b64,String msg) throws Exception{
  String url="https://api.github.com/repos/"+owner+"/"+repo+"/contents/"+path;
  Request g=new Request.Builder().url(url).header("Authorization","Bearer "+token).get().build(); Response rg=client.newCall(g).execute(); String sha=null;
  if(rg.isSuccessful()){ String bd=rg.body().string(); int idx=bd.indexOf("\"sha\":\""); if(idx!=-1){ int s2=idx+7; int e=bd.indexOf("\"",s2); sha=bd.substring(s2,e); } }
  String js="{\"message\":\""+msg+"\",\"content\":\""+b64+"\",\"branch\":\"main\""+(sha!=null?",\"sha\":\""+sha+"\"":"")+"}";
  Request p=new Request.Builder().url(url).header("Authorization","Bearer "+token).put(RequestBody.create(js,MediaType.parse("application/json"))).build(); client.newCall(p).execute();
 }
}
