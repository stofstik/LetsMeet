package com.stofstik.letsmeet;

import android.util.JsonReader;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by stofstik on 15-10-15.
 * TODO Make all of this asynchronous!
 * TODO Use the gson library
 */
public class ResponseParser {

    public String parseLobbyName(String jsonString) throws IOException {
        String lobbyName = null;
        JsonReader reader = new JsonReader(new StringReader(jsonString));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                lobbyName = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return lobbyName;
    }

    public boolean parseLobbyCreator(String jsonString, String userId) throws IOException {
        String creator = "";
        JsonReader reader = new JsonReader(new StringReader(jsonString));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("creator")) {
                creator = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return creator.matches(userId);
    }

    public String parseUserId(String jsonString) throws IOException {
        String userId = null;
        JsonReader reader = new JsonReader(new StringReader(jsonString));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("_id")) {
                userId = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return userId;
    }

    public Lobby parseBla(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Lobby.class);
    }

    public ArrayList<User> parseUsers(String jsonString) throws IOException {
        ArrayList<User> users = new ArrayList<>();

        JsonReader reader = new JsonReader(new StringReader(jsonString));

        // TODO USE GSON!!!

        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("users")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    User user = new User();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("_id")) {
                            user.setId(reader.nextString());
                        } else if (name.equals("creator")) {
                            user.setCreator(reader.nextBoolean());
                        } else if (name.equals("username")) {
                            user.setUsername(reader.nextString());
                        } else if (name.equals("latitude")) {
                            user.setLatitude(reader.nextDouble());
                        } else if (name.equals("longitude")) {
                            user.setLongitude(reader.nextDouble());
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    users.add(user);
                }
                reader.endArray();
            }
        }
        reader.endObject();

        return users;
    }

}
