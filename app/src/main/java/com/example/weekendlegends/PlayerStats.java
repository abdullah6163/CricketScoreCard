package com.example.weekendlegends;

public class PlayerStats {

    // Batting
    public int matches = 0;
    public int runs = 0;
    public int balls = 0;
    public int fours = 0;
    public int sixes = 0;
    public int outs = 0;
    public int highestScore = 0;

    // Bowling
    public int bowlBalls = 0;
    public int bowlRuns = 0;
    public int wickets = 0;

    public float strikeRate() {
        return balls > 0 ? (runs * 100f / balls) : 0f;
    }

    public float economy() {
        return bowlBalls > 0 ? (bowlRuns / (bowlBalls / 6f)) : 0f;
    }
}
