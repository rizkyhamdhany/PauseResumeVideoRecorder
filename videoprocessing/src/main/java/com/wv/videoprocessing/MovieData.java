package com.wv.videoprocessing;

/**
 * Created by hamdhanywijaya@gmail.com on 5/18/17.
 */

public class MovieData {

    private String moviePath;
    private Resolution size;

    public String getMoviePath() {
        return moviePath;
    }

    public void setMoviePath(String moviePath) {
        this.moviePath = moviePath;
    }

    public Resolution getSize() {
        return size;
    }

    public void setSize(Resolution size) {
        this.size = size;
    }

}
