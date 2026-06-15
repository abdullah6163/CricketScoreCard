package com.example.weekendlegends;

public class BallItem {

    public String label = "";
    public boolean isWicket = false;
    public boolean isFour = false;
    public boolean isSix = false;

    public BallItem() {}

    public BallItem(String label, boolean isWicket) {
        this.label = (label == null) ? "" : label.trim();
        this.isWicket = isWicket;

        // ✅ normalize wicket from label too (safety)
        if ("W".equalsIgnoreCase(this.label)) {
            this.isWicket = true;
        }

        // ✅ detect boundary even if label has suffix like "4Wd", "6Nb", "4B"
        if (!this.label.isEmpty()) {
            char c0 = this.label.charAt(0);
            this.isFour = (c0 == '4');
            this.isSix = (c0 == '6');
        }
    }
}
