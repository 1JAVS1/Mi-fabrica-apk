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
  final String WORKFLOW_YML="name: Compilar APK\non: [workflow_dispatch]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    permissions:\n      contents: write\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with:\n          distribution: 'temurin'\n          java-version: '17'\n      - uses: gradle/actions/setup-gradle@v3\n      - name: Build APK\n        run: gradle assembleDebug\n      - name: Release\n        uses: softprops/action-gh-release@v1\n        with:\n          tag_name: v12\n          files: app/build/outputs/apk/debug/app-debug.apk\n        env:\n          GITHUB_TOKEN: ghs_15368_eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhdXRobmQiLCJjdHgiOiJWSnU0RS1tS0R5Sjk0VENzVkNwQWI3UzZ3cDB1dTBqRXgzU2UyUHVpS1FaUGltV3FISWJDcDZzIiwiZXhwIjoxNzg1NzU4MDE5LCJpYXQiOjE3ODU3NTQ0MTksImlzcyI6ImdpdGh1YiIsImp0aSI6Ijg4MDhlOTdkLWJiMWQtNDdkZC05NTI2LWRhMTZhOTZhMWE1YiIsInZlciI6M30.jCVo2YRMFdg8LDH9sGaXJnyH7Dsgj9hxy48-XJgKlBwsUYEs8uI_P8AyDZnL_xyRzqgl0zFAV_iBDHRrBUS2aQ\n";
  @Override protected void onCreate(Bundle b){ super.onCreate(b);
    LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL); lay.setPadding(30,30,30,30);
    TextView t=new TextView(this); t.setText("FABRICA MAESTRA"); t.setTextSize(22);
    edtToken=new EditText(this); edtToken.setHint("Token ghp_...");
    edtRepo=new EditText(this); edtRepo.setHint("Repo"); edtRepo.setText("Mi-fabrica-apk");
    btnPick=new Button(this); btnPick.setText("1. ZIP");
    txtFile=new TextView(this); txtFile.setText("Sin archivo");
    btnCrear=new Button(this); btnCrear.setText("2. CREAR APK");
    txtLog=new TextView(this); txtLog.setText("Log:\n");
    lay.addView(t); lay.addView(edtToken); lay.addView(edtRepo); lay.addView(btnPick); lay.addView(txtFile); lay.addView(btnCrear); lay.addView(txtLog);
    setContentView(lay);
    btnPick.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_GET_CONTENT); i.setType("*/*"); startActivityForResult(i,100); });
    btnCrear.setOnClickListener(v-> crearApk());
  }
  @Override protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req==100 && res==RESULT_OK && data!=null){ zipUri=data.getData(); txtFile.setText("Archivo: "+zipUri.getLastPathSegment()); log("ZIP ok"); } }
  void log(String s){ runOnUiThread(()-> txtLog.append("\n"+s)); }
  void crearApk(){
    String token=edtToken.getText().toString().trim(); String repo=edtRepo.getText().toString().trim();
    if(token.isEmpty()||zipUri==null){ log("Falta token o zip"); return; }
    if(repo.isEmpty()) repo="Mi-fabrica-apk"; String finalRepo=repo;
    Executors.newSingleThreadExecutor().execute(()->{ try{
      log("Verificando..."); Request rqUser=new Request.Builder().url("https://api.github.com/user").header("Authorization","Bearer "+token).build();
      Response rsUser=client.newCall(rqUser).execute(); String bodyUser=rsUser.body().string(); owner=bodyUser.split("\"login\":\"")[1].split("\"")[0];
      log("Usuario: "+owner);
      String wfPath=".github/workflows/compilar.yml"; String wfB64=Base64.encodeToString(WORKFLOW_YML.getBytes(),Base64.NO_WRAP);
      String sha=null; Request rqGet=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+wfPath).header("Authorization","Bearer "+token).build();
      Response rsGet=client.newCall(rqGet).execute(); if(rsGet.isSuccessful()){ String b=rsGet.body().string(); if(b.contains("\"sha\"")) sha=b.split("\"sha\":\"")[1].split("\"")[0]; }
      String jsonWf="{\"message\":\"fabrica\",\"content\":\""+wfB64+"\""+(sha!=null?",\"sha\":\""+sha+"\"":"")+"}";
      Request rqPutWf=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+wfPath).header("Authorization","Bearer "+token).put(RequestBody.create(jsonWf,MediaType.parse("application/json"))).build();
      client.newCall(rqPutWf).execute();
      InputStream is=getContentResolver().openInputStream(zipUri); ByteArrayOutputStream baos=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n; while((n=is.read(buf))!=-1) baos.write(buf,0,n);
      String zipB64=Base64.encodeToString(baos.toByteArray(),Base64.NO_WRAP); String zipPath="project.zip"; String shaZip=null;
      Request rqGetZip=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+zipPath).header("Authorization","Bearer "+token).build();
      Response rsGetZip=client.newCall(rqGetZip).execute(); if(rsGetZip.isSuccessful()){ String bz=rsGetZip.body().string(); if(bz.contains("\"sha\"")) shaZip=bz.split("\"sha\":\"")[1].split("\"")[0]; }
      String jsonZip="{\"message\":\"zip\",\"content\":\""+zipB64+"\""+(shaZip!=null?",\"sha\":\""+shaZip+"\"":"")+"}";
      Request rqPutZip=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/contents/"+zipPath).header("Authorization","Bearer "+token).put(RequestBody.create(jsonZip,MediaType.parse("application/json"))).build();
      client.newCall(rqPutZip).execute();
      String jsonDisp="{\"ref\":\"main\"}"; Request rqDisp=new Request.Builder().url("https://api.github.com/repos/"+owner+"/"+finalRepo+"/actions/workflows/compilar.yml/dispatches").header("Authorization","Bearer "+token).post(RequestBody.create(jsonDisp,MediaType.parse("application/json"))).build();
      client.newCall(rqDisp).execute(); log("Listo! Ve a Releases en 3 min");
    }catch(Exception e){ log("Error: "+e.getMessage()); } });
  }
}
