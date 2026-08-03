
package com.sene.fabrica;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import android.util.Base64;
import java.io.*;
import okhttp3.*;
import org.json.*;
public class MainActivity extends Activity {
  EditText etToken, etRepoName;
  TextView tvLog, tvStatus;
  Button btnDownload;
  Uri zipUri=null;
  OkHttpClient client=new OkHttpClient();
  String lastApkUrl="";
  @Override protected void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(30,30,30,30);
    root.setBackgroundColor(0xFFF5F5F5);
    TextView title=new TextView(this); title.setText("SENE FABRICA V20"); title.setTextSize(22); title.setTextColor(0xFFA0233D); title.setTypeface(null, android.graphics.Typeface.BOLD);
    TextView sub=new TextView(this); sub.setText("Fabrica de APKs - Descarga directa"); sub.setTextSize(12);
    etToken=new EditText(this); etToken.setHint("Token ghp_..."); etToken.setBackgroundColor(0xFFFFFFFF);
    etRepoName=new EditText(this); etRepoName.setHint("Nombre nuevo proyecto ej: mi-tienda"); etRepoName.setBackgroundColor(0xFFFFFFFF);
    Button btnPick=new Button(this); btnPick.setText("1. SELECCIONAR ZIP DEL PROYECTO"); btnPick.setBackgroundColor(0xFF607D8B); btnPick.setTextColor(0xFFFFFFFF);
    Button btnBuild=new Button(this); btnBuild.setText("2. CREAR Y COMPILAR APK"); btnBuild.setBackgroundColor(0xFFA0233D); btnBuild.setTextColor(0xFFFFFFFF);
    tvStatus=new TextView(this); tvStatus.setText("Estado: Esperando ZIP de proyecto"); tvStatus.setTextSize(14); tvStatus.setTypeface(null, android.graphics.Typeface.BOLD); tvStatus.setPadding(20,20,20,20); tvStatus.setBackgroundColor(0xFFFFEB3B);
    btnDownload=new Button(this); btnDownload.setText("⬇️ DESCARGAR APK COMPILADA"); btnDownload.setBackgroundColor(0xFF4CAF50); btnDownload.setTextColor(0xFFFFFFFF); btnDownload.setTextSize(18); btnDownload.setVisibility(Button.GONE);
    tvLog=new TextView(this); tvLog.setText("Log:\n1. Selecciona ZIP de tu proyecto Android\n2. Crea y compila\n3. Descarga APK directo");
    ScrollView sv=new ScrollView(this); sv.addView(tvLog);
    root.addView(title); root.addView(sub); root.addView(etToken); root.addView(etRepoName); root.addView(btnPick); root.addView(btnBuild); root.addView(tvStatus); root.addView(btnDownload); root.addView(sv);
    setContentView(root);
    btnPick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("application/zip"); startActivityForResult(i,1001); });
    btnBuild.setOnClickListener(v->{ if(zipUri==null){ setStatus("Selecciona ZIP primero",0xFFFF9800); return; } String t=etToken.getText().toString().trim(); String r=etRepoName.getText().toString().trim(); if(t.isEmpty()||r.isEmpty()){ setStatus("Falta token o nombre",0xFFFF9800); return; } btnDownload.setVisibility(Button.GONE); new Thread(()->{ try{ crear(t,"1JAVS1",r); }catch(Exception e){ log("Error: "+e.getMessage()); setStatus("Error",0xFFF44336); } }).start(); });
    btnDownload.setOnClickListener(v->{ if(!lastApkUrl.isEmpty()){ log("Iniciando descarga directa..."); DownloadManager dm=(DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE); DownloadManager.Request req=new DownloadManager.Request(Uri.parse(lastApkUrl)); req.setTitle("SENE APK Compilada"); req.setDescription("Descargando..."); req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, etRepoName.getText().toString().trim()+".apk"); dm.enqueue(req); Toast.makeText(this, "Descarga iniciada en Descargas", Toast.LENGTH_LONG).show(); setStatus("Descarga iniciada - Revisa notificaciones",0xFF4CAF50); } });
  }
  void log(String s){ runOnUiThread(()-> tvLog.append("\n"+s)); }
  void setStatus(String s, int color){ runOnUiThread(()-> { tvStatus.setText("Estado: "+s); tvStatus.setBackgroundColor(color); }); }
  @Override protected void onActivityResult(int rc,int res,Intent data){ super.onActivityResult(rc,res,data); if(rc==1001&&data!=null){ zipUri=data.getData(); log("ZIP: "+data.getData().getLastPathSegment()); setStatus("ZIP listo: "+data.getData().getLastPathSegment(),0xFF8BC34A); } }
  void crear(String token,String owner,String repo) throws Exception {
    setStatus("Creando repo "+repo,0xFF03A9F4); log("Creando repo "+repo);
    String json="{\"name\":\""+repo+"\",\"private\":false}"; Request req=new Request.Builder().url("https://api.github.com/user/repos").header("Authorization","Bearer "+token).post(RequestBody.create(json,MediaType.parse("application/json"))).build(); Response r=client.newCall(req).execute(); log("Repo: "+r.code());
    setStatus("Subiendo ZIP...",0xFF03A9F4);
    InputStream is=getContentResolver().openInputStream(zipUri); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=is.read(buf))!=-1) baos.write(buf,0,n); String b64=Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    log("ZIP "+(b64.length()/1024)+" KB b64");
    putFile(token,owner,repo,"project.zip.b64",b64,"ZIP SENE");
    String wf="name: Compilar APK SENE\non: [workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n        with:\n          gradle-version: 8.5\n      - name: Preparar\n        run: |\n          base64 -d project.zip.b64 > project.zip\n          unzip -o project.zip -d .\n          rm project.zip.b64 project.zip\n      - name: Compilar\n        run: gradle assembleDebug --stacktrace\n      - name: Release\n        uses: softprops/action-gh-release@v1\n        with:\n          tag_name: sene-v${{ github.run_number }}\n          files: app/build/outputs/apk/debug/app-debug.apk\n        env:\n          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n";
    putFile(token,owner,repo,".github/workflows/compilar.yml",Base64.encodeToString(wf.getBytes(), Base64.NO_WRAP),"Workflow");
    Request disp=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/compilar.yml/dispatches").header("Authorization","Bearer "+token).post(RequestBody.create("{\"ref\":\"main\"}",MediaType.parse("application/json"))).build(); client.newCall(disp).execute();
    setStatus("Compilando en nube 3 min...",0xFFFFEB3B);
    for(int i=0;i<20;i++){
      Thread.sleep(20000);
      setStatus("Compilando "+((i+1)*20)+"s...",0xFFFFEB3B);
      try{
        Request check=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+repo+"/releases/latest").header("Authorization","Bearer "+token).get().build(); Response cr=client.newCall(check).execute();
        if(cr.isSuccessful()){
          String body=cr.body().string();
          JSONObject jo=new JSONObject(body);
          if(jo.has("assets")){
            JSONArray arr=jo.getJSONArray("assets");
            if(arr.length()>0){
              lastApkUrl=arr.getJSONObject(0).getString("browser_download_url");
              setStatus("¡APK LISTA! Toca DESCARGAR",0xFF4CAF50);
              log("APK lista: "+lastApkUrl);
              runOnUiThread(()-> btnDownload.setVisibility(Button.VISIBLE));
              return;
            }
          }
        }
      }catch(Exception e){ log("Esperando... "+e.getMessage()); }
    }
    setStatus("Tiempo agotado - Revisa Releases manual",0xFFFF9800);
  }
  void putFile(String token,String owner,String repo,String path,String b64,String msg) throws Exception {
    String url="https://api.github.com/repos/"+owner+"/"+repo+"/contents/"+path;
    Request get=new Request.Builder().url(url).header("Authorization","Bearer "+token).get().build(); Response rg=client.newCall(get).execute(); String sha=null; if(rg.isSuccessful()){ String body=rg.body().string(); int idx=body.indexOf("\"sha\":\""); if(idx!=-1){ int s=idx+7; int e=body.indexOf("\"",s); sha=body.substring(s,e); } }
    String json="{"+(sha!=null?"\"sha\":\""+sha+"\",":"")+"\"message\":\""+msg+"\",\"content\":\""+b64+"\",\"branch\":\"main\"}";
    Request put=new Request.Builder().url(url).header("Authorization","Bearer "+token).put(RequestBody.create(json,MediaType.parse("application/json"))).build(); Response rp=client.newCall(put).execute(); log(path+" -> "+rp.code());
  }
}
