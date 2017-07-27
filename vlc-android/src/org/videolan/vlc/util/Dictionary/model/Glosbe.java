
package org.videolan.vlc.util.Dictionary.model;

import android.util.Log;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Glosbe {

    @SerializedName("result")
    @Expose
    private String result;
    @SerializedName("tuc")
    @Expose
    private List<Tuc> tuc = null;
    @SerializedName("phrase")
    @Expose
    private String phrase;
    @SerializedName("from")
    @Expose
    private String from;
    @SerializedName("dest")
    @Expose
    private String dest;
//    @SerializedName("authors")
//    @Expose
//    private Authors authors;
    @SerializedName("authors")
    @Expose
    private Map<String, Author> authors;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<Tuc> getTuc() {
        return tuc;
    }

    public void setTuc(List<Tuc> tuc) {
        this.tuc = tuc;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public Map<String, Author> getAuthors() {
        return authors;
    }
//    public void setAuthors(Map<String, Author> authors) {
//        Log.d("status","setAuthors is called");
//        this.authors = authors;
//    }

}
