package org.tensorflow.lite.examples.detection;

public class Point {
    private int id;
    private String name;
    private double Rlatitude;
    private double Rlongitude;

    //id
    public void setId(int id){
        this.id = id;
    }
    public int getId(){
        return id;
    }

    //name
    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return name;
    }

    //Rlatitude
    public void setRlatitude(double Rlatitude){
        this.Rlatitude = Rlatitude;
    }
    public double getRlatitude(){
        return Rlatitude;
    }

    //Rlongitude
    public void setRlongitude(double Rlongitude){
        this.Rlongitude = Rlongitude;
    }
    public double getRlongitude(){
        return Rlongitude;
    }

}
