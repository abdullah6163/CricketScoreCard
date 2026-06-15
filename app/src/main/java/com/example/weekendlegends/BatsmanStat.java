package com.example.weekendlegends;

public class BatsmanStat {
    public String name;
    public int r, b, fours, sixes;

    public BatsmanStat(String name) {
        this.name = name;
    }

    public double sr() {
        if (b <= 0) return 0.0;
        return (r * 100.0) / b;
    }
}
