package org.videolan.vlc.util.Dictionary.model;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Tuc {

    @SerializedName("phrase")
    @Expose
    private Phrase phrase;
    @SerializedName("meanings")
    @Expose
    private List<Meaning> meanings = null;
    @SerializedName("meaningId")
    @Expose
    private long meaningId;
    @SerializedName("authors")
    @Expose
    private List<Integer> authors = null;

    public Phrase getPhrase() {
        return phrase;
    }

    public void setPhrase(Phrase phrase) {
        this.phrase = phrase;
    }

    public List<Meaning> getMeanings() {
        return meanings;
    }

    public void setMeanings(List<Meaning> meanings) {
        this.meanings = meanings;
    }

    public long getMeaningId() {
        return meaningId;
    }

    public void setMeaningId(Integer meaningId) {
        this.meaningId = meaningId;
    }

    public List<Integer> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Integer> authors) {
        this.authors = authors;
    }

}
