
package org.videolan.vlc.util.Dictionary.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Author {

    @SerializedName("U")
    @Expose
    private String u;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("N")
    @Expose
    private String n;
    @SerializedName("url")
    @Expose
    private String url;

    public String getU() {
        return u;
    }

    public void setU(String u) {
        this.u = u;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
