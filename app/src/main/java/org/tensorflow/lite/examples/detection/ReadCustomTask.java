package org.tensorflow.lite.examples.detection;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

//Custom task
public class ReadCustomTask{// extends AsyncTask<String, Void, String> {
    String sendMsg, receiveMsg;
    boolean key = false;


    private String result;   // for receivedMSG from ReadCustomTask
    private boolean flag_result_received;

    public void init(){
        flag_result_received = false;
        result = "";
        key = true;
    }

//    @Override
//    protected String doInBackground(String... strings) {
//        try {
//            while (true) {
//                synchronized (this) {
    public String test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    if (key != true);
//                        this.wait();
            URL url = new URL("http://192.168.176.221:8080/TestJSP_war_exploded/read.jsp");
//                    URL url = new URL("http://192.168.200.109:8080/TestJSP_war_exploded/read.jsp");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestMethod("POST");
                    OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());

                    String str;
                    try {
                        sendMsg = "latitude=" + AppGlobal.getConfig().getLatitude() + "&longitude=" + AppGlobal.getConfig().getLongitude();
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
                            result = receiveMsg;

                        } else {
                            Log.i("통신 결과", conn.getResponseCode() + "에러");
                            result = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    setFlag_result_received(true);

                    setKey(false);
                    setPriority(false);
//                    notifyAll();
//                    this.wait();
//                }
//            }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return receiveMsg;  // Unused dummy code

    }

    public void setFlag_result_received(boolean TorF){
        flag_result_received = TorF;
    }
    public boolean getFlag_result_received(){
        return flag_result_received;
    }
    public String getResult(){
        return result;
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