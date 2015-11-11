package com.stofstik.letsmeet;

import java.util.Calendar;

/**
 * Created by stofstik on 29-10-15.
 */
public class Lobby {
    String name, creator;
    User[] users;
    Calendar expiration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public User[] getUsers() {
        return users;
    }

    public void setUsers(User[] users) {
        this.users = users;
    }

    public Calendar getExpiration() {
        return expiration;
    }

    public void setExpiration(Calendar expiration) {
        this.expiration = expiration;
    }
}
