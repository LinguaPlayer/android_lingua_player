
package org.videolan.vlc.util.Dictionary.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Meaning {

    @SerializedName("language")
    @Expose
    private String language;
    @SerializedName("text")
    @Expose
    private String text;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
