package de.fs.fintech.geogame.data;

import java.util.HashMap;

public class User {

    public String displayName;
    public String email;
    public String pseudonym;

    public HashMap<String,PortalUnique> uniques;
    
    public User() {

    }

    public User(String email, String displayName) {
        this.displayName=displayName;
        this.email=email;
    }
}
