package com.example.weekendlegends;

public class OverListItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_OVER = 1;

    public int type;
    public String headerText;
    public OverItem overItem;

    public static OverListItem header(String text) {
        OverListItem x = new OverListItem();
        x.type = TYPE_HEADER;
        x.headerText = text;
        return x;
    }

    public static OverListItem over(OverItem oi) {
        OverListItem x = new OverListItem();
        x.type = TYPE_OVER;
        x.overItem = oi;
        return x;
    }
}
