package com.example.weekendlegends;

public class BowlerStat {
    public String name;
    public int balls;     // total balls (legal)
    public int maidens;
    public int runs;
    public int wkts;

    public BowlerStat(String name) {
        this.name = name;
    }

    public String oversText() {
        int o = balls / 6;
        int b = balls % 6;
        return o + "." + b;
    }

    public double er() {
        double overs = (balls / 6) + ((balls % 6) / 6.0);
        if (overs <= 0) return 0.0;
        return runs / overs;
    }
}
