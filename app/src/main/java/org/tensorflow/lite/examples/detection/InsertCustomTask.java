package org.tensorflow.lite.examples.detection;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class InsertCustomTask{//} extends AsyncTask<String, Void, String> {
    String sendMsg, receiveMsg;
    boolean key = false;

//    private double cur_Lat;
//    private double cur_Lon;
//
//    private double oldLon;
//    private double oldLat;
//
//    public void init(){
//        cur_Lat = AppGlobal.getConfig().getLatitude();
//        cur_Lon = AppGlobal.getConfig().getLongitude();
//        oldLon = cur_Lon;
//        oldLat = cur_Lat;
//    }

//    @Override
//    protected String doInBackground(String... strings) {

    public String test() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//            while (true) {
//                synchronized (this) {
                    System.out.println("[insert]before wait");
                    Log.i("TEST", String.format("BEFORE"));


//                    if (key != true) ;
//                        this.wait();
                    System.out.println("[insert]after wait");
                    Log.i("TEST", String.format("AFTER"));


//            URL url = new URL("http://192.168.176.221:8080/TestJSP_war_exploded/insert.jsp");
                    URL url = new URL("http://192.168.200.109:8080/TestJSP_war_exploded/insert.jsp");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestMethod("POST");
                    OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());


                    String str;

                    try {
                        sendMsg = "name=bollard&latitude=" + AppGlobal.getConfig().getLatitude() + "&longitude=" + AppGlobal.getConfig().getLongitude();
                        osw.write(sendMsg);
                        osw.flush();
                        if (conn.getResponseCode() == conn.HTTP_OK) {
                            InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                            BufferedReader reader = new BufferedReader(tmp);
                            StringBuffer buffer = new StringBuffer();
                            while ((str = reader.readLine()) != null) {
                                buffer.append(str);
                            }
                            receiveMsg = buffer.toString();

                        } else {
                            Log.i("통신 결과", conn.getResponseCode() + "에러");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    setKey(false);
                    setPriority(false);
                    //            notifyAll();
//                    this.wait();
//                }
//            }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return receiveMsg;

    }

            public void setKey(boolean TorF){
        key = TorF;
    }
    public void setPriority(boolean TorF){
        if(TorF)
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        else
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    }
}