package com.fabrica.maestra;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;
import java.io.*;
import java.util.concurrent.Executors;
import okhttp3.*;
public class MainActivity extends Activity {
  EditText edtToken, edtRepo; TextView txtFile, txtLog; Button btnPick, btnCrear; Uri zipUri=null;
  OkHttpClient client=new OkHttpClient(); String owner="";
  final String WORKFLOW_YML="name: Compilar APK\non: [workflow_dispatch]\njobs:\n  compilar:\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - name: Descomprimir proyecto del usuario\n        run: |\n          if [ -f project.zip ]; then\n            unzip -o project.zip -d .\n            rm project.zip\n          fi\n      - name: Compilar\n        run: |\n          chmod +x gradlew || true\n          ./gradlew assembleDebug || gradle assembleDebug\n      - name: Subir APK\n        uses: softprops/action-gh-release@v1\n        with:\n          tag_name: v11\n          files: app/build/outputs/apk/debug/app-debug.apk\n        env:\n          GITHUB_TOKEN: ghs_15368_eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhdXRobmQiLCJjdHgiOiJ2eFZ2ek1KSUZfSlVQOTQ0cFd0TFZoXzVqVmVJekZJYkduSjdzSmJ5d2FkU3FCZE1HYjNsekZFIiwiZXhwIjoxNzg1NzU3NTk3LCJpYXQiOjE3ODU3NTM5OTcsImlzcyI6ImdpdGh1YiIsImp0aSI6IjAwZGE2ZDFiLTY4YzUtNDhhYS05YjcxLTM3MTI2NThlYjQ5OCIsInZlciI6M30.4pLT5QxqIF8NzYAnLk6NHuOHMasvIVC0aEx9I0zIH3NDJeUCHgNxegYn6v0Zck4a2WvpZqUu29X4THoYhhavCg\n";
  @Override protected void onCreate(Bundle b){ super.onCreate(b);
    LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL); lay.setPadding(30,30,30,30);
    TextView t=new TextView(this); t.setText("FABRICA MAESTRA APK"); t.setTextSize(22);
    edtToken=new EditText(this); edtToken.setHint("Pega tu token ghp_...");
    edtRepo=new EditText(this); edtRepo.setHint("Repo (ej: Mi-fabrica-apk)"); edtRepo.setText("Mi-fabrica-apk");
    btnPick=new Button(this); btnPick.setText("1. Seleccionar ZIP de tu proyecto");
    txtFile=new TextView(this); txtFile.setText("Ningun archivo seleccionado");
    btnCrear=new Button(this); btnCrear.setText("2. CREAR APK y subir a GitHub");
    txtLog=new TextView(this); txtLog.setText("Log:\n"); txtLog.setTextSize(12);
    lay.addView(t); lay.addView(edtToken); lay.addView(edtRepo); lay.addView(btnPick); lay.addView(txtFile); lay.addView(btnCrear); lay.addView(txtLog);
    setContentView(lay);
    btnPick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,100); });
    btnCrear.setOnClickListener(v-> crearApk());
  }
  @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==100 && res==RESULT_OK && data!=null){ zipUri=data.getData(); txtFile.setText("Archivo: "+zipUri.getLastPathSegment()); log("ZIP seleccionado"); } }
  void log(String s){ runOnUiThread(()-> txtLog.append("\n"+s)); }
  void crearApk(){
    String token=edtToken.getText().toString().trim(); String repo=edtRepo.getText().toString().trim();
    if(token.isEmpty()||zipUri==null){ log("Falta token o zip"); return; }
    if(repo.isEmpty()) repo="Mi-fabrica-apk"; String finalRepo=repo;
    Executors.newSingleThreadExecutor().execute(()->{ try{
      log("1. Verificando token..."); Request rqUser=new Request.Builder().url("https://api.github.com/user").header("Authorization","Bearer "+token).build();
      Response rsUser=client.newCall(rqUser).execute(); if(!rsUser.isSuccessful()){ log("Token invalido: "+rsUser.code()); return; }
      String bodyUser=rsUser.body().string(); owner=bodyUser.split("\"login\":\"")[1].split("\"")[0]; log("Usuario: "+owner);
      log("2. Preparando fabrica..."); String wfPath=".github/workflows/compilar.yml"; String wfB64=Base64.encodeToString(WORKFLOW_YML.getBytes(),Base64.NO_WRAP);
      String sha=null; Request rqGet=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+wfPath).header("Authorization","Bearer "+token).build();
      Response rsGet=client.newCall(rqGet).execute(); if(rsGet.isSuccessful()){ String b=rsGet.body().string(); if(b.contains("\"sha\"")) sha=b.split("\"sha\":\"")[1].split("\"")[0]; }
      String jsonWf="{\"message\":\"Actualizar fabrica maestra\",\"content\":\""+wfB64+"\""+(sha!=null?",\"sha\":\""+sha+"\"":"")+"}";
      Request rqPutWf=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+wfPath).header("Authorization","Bearer "+token).put(RequestBody.create(jsonWf,MediaType.parse("application/json"))).build();
      client.newCall(rqPutWf).execute(); log("Fabrica lista");
      log("3. Subiendo tu proyecto ZIP..."); InputStream is=getContentResolver().openInputStream(zipUri); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=is.read(buf))!=-1) baos.write(buf,0,n);
      String zipB64=Base64.encodeToString(baos.toByteArray(),Base64.NO_WRAP); String zipPath="project.zip"; String shaZip=null;
      Request rqGetZip=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+zipPath).header("Authorization","Bearer "+token).build();
      Response rsGetZip=client.newCall(rqGetZip).execute(); if(rsGetZip.isSuccessful()){ String bz=rsGetZip.body().string(); if(bz.contains("\"sha\"")) shaZip=bz.split("\"sha\":\"")[1].split("\"")[0]; }
      String jsonZip="{\"message\":\"Subir proyecto para compilar\",\"content\":\""+zipB64+"\""+(shaZip!=null?",\"sha\":\""+shaZip+"\"":"")+"}";
      Request rqPutZip=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+zipPath).header("Authorization","Bearer "+token).put(RequestBody.create(jsonZip,MediaType.parse("application/json"))).build();
      Response rsPutZip=client.newCall(rqPutZip).execute(); if(!rsPutZip.isSuccessful()){ log("Error subiendo ZIP: "+rsPutZip.code()+" "+rsPutZip.body().string()); return; } log("ZIP subido!");
      log("4. Iniciando compilacion en GitHub..."); String jsonDisp="{\"ref\":\"main\"}"; Request rqDisp=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/actions/workflows/compilar.yml/dispatches").header("Authorization","Bearer "+token).post(RequestBody.create(jsonDisp,MediaType.parse("application/json"))).build();
      client.newCall(rqDisp).execute(); log("Compilacion iniciada. Ve a GitHub > Actions. En 3 min tendras tu APK en Releases.\nRepo: https://github.com/"+owner+"/"+finalRepo+"/releases");
    }catch(Exception e){ log("Error: "+e.getMessage()); e.printStackTrace(); } });
  }
}
