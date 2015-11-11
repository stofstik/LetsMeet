package com.stofstik.letsmeet;

/**
 * Created by stofstik on 15-10-15.
 */
public class User {

    String id;
    Double latitude, longitude;
    String username;
    boolean creator;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isCreator() {
        return creator;
    }

    public void setCreator(boolean creator) {
        this.creator = creator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
