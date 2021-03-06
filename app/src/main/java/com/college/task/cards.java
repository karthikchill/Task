package com.college.task;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
public class cards {
    private String FILE;
    private String DUE;
    private String TITLE;
    private String DESC;

    @NonNull
    @Override
    public String toString() {
        return "cards{" +
                ", DUE='" + DUE + '\'' +
                ", TITLE='" + TITLE + '\'' +
                ", DESC='" + DESC + '\'' +
                '}';
    }

    public cards(String DUE, String TITLE, String DESC) {
        this.DUE = DUE;
        this.TITLE = TITLE;
        this.DESC = DESC;
    }

    public String getDESC() {
        return DESC;
    }

    public void setDESC(String DESC) {
        this.DESC = DESC;
    }


    public String getTITLE() {
        return TITLE;
    }

    public void setTITLE(String TITLE) {
        this.TITLE = TITLE;
    }


    public String getDUE() {
        return DUE;
    }

    public void setDUE(String DUE) {
        this.DUE = DUE;
    }


}
