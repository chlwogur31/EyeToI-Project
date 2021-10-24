package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.speech.tts.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PoI extends GpsTracker{

    private List<Point> pointList = new ArrayList<>();

    private ReadCustomTask readCustomTask;
    private InsertCustomTask insertCustomTask;
    private Calc calc;

    // 100미터 반경 가져오기
    private double fixlat;
    private double fixlon;
    private double cur_lat;
    private double cur_lon;
    private double old_lat;
    private double old_lon;

    private List<Point> newAngleList;
    private List<Point> oldAngleList = new ArrayList<>();
    private List<Point> resultAngleList;
    private int cnt = 1;

    private TextToSpeech textToSpeech;

    public PoI(Context context) {
        super(context);
    }

    public void init(){

//        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
//            @Override
//            public void onInit(int status){
//                if(status != ERROR)
//                    textToSpeech.setLanguage(Locale.KOREAN);
//            }
//        });
        textToSpeech = AppGlobal.getConfig().getTts();

        calc = new Calc();

        getLocation();
        cur_lat = getLatitude();
        cur_lon = getLongitude();
        fixlat = cur_lat;
        fixlon = cur_lon;
        old_lat = cur_lat;
        old_lon = cur_lon;

        readCustomTask = new ReadCustomTask();
        insertCustomTask = new InsertCustomTask();

        AppGlobal.getConfig().setLatitude(cur_lat);
        AppGlobal.getConfig().setLongitude(cur_lon);
        AppGlobal.getConfig().setAngle(0);

        readCustomTask.init();
//        insertCustomTask.init();
//        readCustomTask.execute();
        System.out.println("[insert] before insert execute");
//        insertCustomTask.execute();
        System.out.println("[insert] after insert execute");
    }


    public void startCalc() {

        System.out.println("fixlat:" + fixlat + ", fixlon:" + fixlon);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    System.out.println("============ while" + cnt + " =========");

                    //현재 gps 위치 갱신
                    getLocation();
                    cur_lat = getLatitude();
                    AppGlobal.getConfig().setLatitude(cur_lat);
                    cur_lon = getLongitude();
                    AppGlobal.getConfig().setLongitude(cur_lon);

                    if(old_lat != cur_lat || old_lon != cur_lon){
                        AppGlobal.getConfig().setAngle(bearingP1toP2(old_lat,old_lon,cur_lat,cur_lon));
                        System.out.println("방위각 각도 : "+bearingP1toP2(old_lat,old_lon,cur_lat,cur_lon));
                        System.out.println("이동하기 전에 old_lat: "+ old_lat + " old_lon: "+old_lon);
                        old_lat = cur_lat;
                        old_lon = cur_lon;
                        System.out.println("이동을 한 후에 old_lat: "+ old_lat + " old_lon: "+old_lon);
                    }

                    System.out.println("now latitutde:" + cur_lat + ", now longitude:" + cur_lon);

                    //read했던곳과 현재위치 거리계산
                    //distnce 80넘으면 read새로하기
                    //fixlocation변경
                    double dist = getDistance(fixlat, fixlon, cur_lat, cur_lon);
                    System.out.println("<거리dist>:" + dist);
                    if (dist >= 80) {
                        readLocation();

                        fixlat = cur_lat;
                        fixlon = cur_lon;
                        System.out.println("[if]fixlat:" + fixlat + ", fixlon:" + fixlon);
                    }

                    //100m반경내에 전방 삼각형 범위안에 드는 list 계산
                    //(anglelist && !oldAngleList) => TTS로 음성출력
                    newAngleList = calcCC(cur_lat, cur_lon, AppGlobal.getConfig().getAngle(), pointList);
                    for (Point p : newAngleList) {
                        System.out.println("newAngleList:" + p.getRlatitude() + ", " + p.getRlongitude());
//                        if (!oldAngleList.contains(p)) {
                        textToSpeech.speak(String.format("볼라드가 전방 삼미터내에 있습니다"), TextToSpeech.QUEUE_ADD, null, null);
                        break;
//                        }
                    }

//                    for (Point p : oldAngleList) {
//                        System.out.println("oldAngleList:" + p.getRlatitude() + ", " + p.getRlongitude());
//                    }
//                    oldAngleList = newAngleList;
                    cnt++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }


    public class Calc {
        double Lx; //x좌표
        double Ly; //y좌표
        double Rx; //x좌표
        double Ry; //y좌표

        static final double dLa = 0.00004504995306; //위도에서 5미터 수치
        static final double dLo = 0.0000565969392375; // 경도에서 5미터 수치

        public double[] geoMove(double latitude, double longitude, double direction_degree, double length_degree) {
            double[] location = new double[2];

            length_degree = (0.00001 / 1.11) * length_degree;

            double x = longitude + length_degree * Math.cos(direction_degree * Math.PI / 180);
            double y = latitude + length_degree * Math.sin(direction_degree * Math.PI / 180);

            location[0] = x;
            location[1] = y;

            return location;
        }

        public List<Point> cc(double ltt, double lgt, double angle, List<Point> pointList){

//      System.out.println("latitude : " + ltt);
//      System.out.println("longitude : " + lgt);
//      System.out.println("angle : " + angle);

            List<Point> newtds = new ArrayList<>();

            if(0 <= angle && angle < 45){ //1사분면
                Lx = ltt - dLa * Math.sin(Math.toRadians(45 - angle));
                Ly = lgt + dLo * Math.cos(Math.toRadians(45 - angle));
                Rx = ltt + dLa * Math.sin(Math.toRadians(angle + 45));
                Ry = lgt + dLo * Math.cos(Math.toRadians(angle + 45));

            }else if(45 <= angle && angle < 90){ //1사분면
                Lx = ltt + dLa * Math.sin(Math.toRadians(angle - 45));
                Ly = lgt + dLo * Math.cos(Math.toRadians(angle - 45));
                Rx = ltt + dLa * Math.cos(Math.toRadians(angle - 45));
                Ry = lgt - dLo * Math.sin(Math.toRadians(angle - 45));

            }else if(90 <= angle && angle < 135){ //2사분면
                Lx = ltt + dLa * Math.cos(Math.toRadians(135 - angle));
                Ly = lgt + dLo * Math.sin(Math.toRadians(135 - angle));
                Rx = ltt + dLa * Math.sin(Math.toRadians(135 - angle));
                Ry = lgt - dLo * Math.cos(Math.toRadians(135 - angle));

            }else if(135 <= angle && angle < 180){ //2사분면
                Lx = ltt + dLa * Math.cos(Math.toRadians(angle - 135));
                Ly = lgt - dLo * Math.sin(Math.toRadians(angle - 135));
                Rx = ltt - dLa * Math.sin(Math.toRadians(angle - 135));
                Ry = lgt - dLo * Math.cos(Math.toRadians(angle - 135));

            }else if(180 <= angle && angle < 225){ //3사분면
                Lx = ltt + dLa * Math.sin(Math.toRadians(225 - angle));
                Ly = lgt - dLo * Math.cos(Math.toRadians(225 - angle));
                Rx = ltt - dLa * Math.cos(Math.toRadians(225 - angle));
                Ry = lgt - dLo * Math.sin(Math.toRadians(225 - angle));

            }else if(225 <= angle && angle < 270){ //3사분면
                Lx = ltt - dLa * Math.sin(Math.toRadians(angle - 225));
                Ly = lgt - dLo * Math.cos(Math.toRadians(angle - 225));
                Rx = ltt - dLa * Math.cos(Math.toRadians(angle - 225));
                Ry = lgt + dLo * Math.sin(Math.toRadians(angle - 225));

            }else if(270 <= angle && angle < 315){ //4사분면
                Lx = ltt - dLa * Math.cos(Math.toRadians(315 - angle));
                Ly = lgt - dLo * Math.sin(Math.toRadians(315 - angle));
                Rx = ltt - dLa * Math.sin(Math.toRadians(315 - angle));
                Ry = lgt + dLo * Math.cos(Math.toRadians(315 - angle));

            }else if(315 <= angle && angle < 360){ //4사분면
                Lx = ltt - dLa * Math.cos(Math.toRadians(angle - 315));
                Ly = lgt + dLo * Math.sin(Math.toRadians(angle - 315));
                Rx = ltt + dLa * Math.sin(Math.toRadians(angle - 315));
                Ry = lgt + dLo * Math.cos(Math.toRadians(angle - 315));

            }

//            Lx = geoMove(ltt, lgt, angle - 45, d)[1];
//            Ly = geoMove(ltt, lgt, angle - 45, d)[0];
//            Rx = geoMove(ltt, lgt, angle + 45, d)[1];
//            Ry = geoMove(ltt, lgt, angle + 45, d)[0];

            System.out.println("Lx :  "+ Lx + " Ly : " + Ly);
            System.out.println("Rx :  "+ Rx + " Ry : " + Ry);
//      System.out.println("=======================");
//      System.out.println("angle:"+angle);
//      System.out.println("Lx:"+Lx);
//      System.out.println("Ly:"+Ly);
//      System.out.println("Rx:"+Rx);
//      System.out.println("Ry:"+Ry);

            for(Point td : pointList) {
                double x = td.getRlatitude();
                double y = td.getRlongitude();
                if (isUp(Lx, Ly, Rx, Ry, x, y) && isUp(Rx, Ry, ltt, lgt, x, y) && isUp(ltt, lgt, Lx, Ly, x, y)) {
                    System.out.println("<삼각형 안에 있다>");

                    newtds.add(td);
                } else
                    System.out.println("<삼각형 밖에 있다>");
            }

            return newtds;
        }

        public boolean isUp(double Lx, double Ly, double Rx, double Ry, double x, double y){
            return ((Ry - Ly) * x + (Lx - Rx) * y + Rx * Ly - Lx * Ry) > 0; //선분RL
        }

    }

    public void readLocation() {   //첫 위치와 거리를 잴 때 설정된 반경을 벗어날 때만 이 함수를 호출!!!

        try {
            pointList.clear();
            // read 요청 보내기
            read();

            // result 받을 때까지 대기
            while(true) {
                if (readCustomTask.getFlag_result_received()) {

                    JSONObject json = null;
                    if(readCustomTask.getResult() != null) {
                        json = new JSONObject(readCustomTask.getResult());
                        JSONArray jsonArray = new JSONArray(json.getString("list"));
                        for (int i = 0; i < jsonArray.length(); i++) {
                            // Array 에서 하나의 JSONObject 를 추출
                            JSONObject dataJsonObject = jsonArray.getJSONObject(i);
                            // 추출한 Object 에서 필요한 데이터를 표시할 방법을 정해서 화면에 표시
                            // 필자는 RecyclerView 로 데이터를 표시 함
                            Point point = new Point();
                            point.setId(dataJsonObject.getInt("id"));
                            point.setName(dataJsonObject.getString("name"));
                            point.setRlatitude(dataJsonObject.getDouble("latitude"));
                            point.setRlongitude(dataJsonObject.getDouble("longitude"));
                            pointList.add(point);

                        }
                        for (Point p : pointList)
                            System.out.println("pointList :" + p.getRlatitude() + ", " + p.getRlongitude());

                        readCustomTask.setFlag_result_received(false);
                        break;  // while break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void insert(){
        System.out.println("[insert] in insert method");
        getLocation();
        AppGlobal.getConfig().setLatitude(getLatitude());
        AppGlobal.getConfig().setLongitude(getLongitude());
//        synchronized (insertCustomTask) {
//            insertCustomTask.setKey(true);
//            readCustomTask.setPriority(true);
//            insertCustomTask.notifyAll();
//
//        }
        insertCustomTask.test();
//        synchronized (this){
//            try {
//                this.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }
    public void read(){
//        synchronized (readCustomTask) {
//            readCustomTask.setKey(true);
//            readCustomTask.setPriority(true);
//            readCustomTask.notifyAll();
//        }
        readCustomTask.test();
//        synchronized (this){
//            try {
//                this.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public List<Point> calcCC(double ltt, double lgt, double angle, List<Point> pointList){
        return calc.cc(ltt,lgt,angle,pointList);
    }


    public double getDistance(double lat1 , double lng1 , double lat2 , double lng2 ){
        double distance;

        Location locationA = new Location("point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lng1);

        Location locationB = new Location("point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lng2);

        distance = locationA.distanceTo(locationB);

        return distance;
    }

//    public static double getdistance2(double lat1, double lon1, double lat2, double lon2) {
//
//        double theta = lon1 - lon2;
//        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
//                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
//
//        dist = Math.acos(dist);
//        dist = rad2deg(dist);
//
//        dist = dist * 60 * 1.1515;
//
//        dist = dist * 1609.344;
//
//        //return (Math.round(dist/10)*10);
//        return dist;
//    }

    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }



    /**
     * 두 좌표 방위각 구하기
     * @param latitude1 Start latitude
     * @param longitude1 Start longitude
     * @param latitude2 End latitude
     * @param longitude2 End longitude
     * @return bearing
     */

    public static short bearingP1toP2(double latitude1, double longitude1, double latitude2, double longitude2) {
        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Cur_Lat_radian = latitude1 * (Math.PI / 180);
        double Cur_Lon_radian = longitude1 * (Math.PI / 180);


        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Dest_Lat_radian = latitude2 * (Math.PI / 180);
        double Dest_Lon_radian = longitude2 * (Math.PI / 180);

        // radian distance
        double radian_distance = 0;
        radian_distance = Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian)
                + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안값이다.
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian)
                * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));// acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing = 0;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0) {
            true_bearing = radian_bearing * (180 / Math.PI);
            true_bearing = 360 - true_bearing;
        } else {
            true_bearing = radian_bearing * (180 / Math.PI);
        }

        return (short) true_bearing;
    }

}
