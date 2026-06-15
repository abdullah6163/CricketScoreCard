package com.example.weekendlegends;

public class Match {

    public String id;
    public String title;
    public String dateText;
    public long dateUtc;
    public String status;     // in_progress / completed
    public String matchJson;  // full saved match

    public Match(String id,
                 String title,
                 String dateText,
                 long dateUtc,
                 String status,
                 String matchJson) {

        this.id = id;
        this.title = title;
        this.dateText = dateText;
        this.dateUtc = dateUtc;
        this.status = status;
        this.matchJson = matchJson;
    }
}
