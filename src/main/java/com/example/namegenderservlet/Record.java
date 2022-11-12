package com.example.namegenderservlet;

public class Record {
    public String name, gender;
    public double prob;
    public int cnt;
    public Record(String name, String gender, double prob, int cnt) {
        this.name = name;
        this.gender = gender;
        this.prob = prob;
        this.cnt = cnt;
    }
}
