package appcontest.sorrysori;

import java.util.HashMap;
import java.util.Map;

public class User {
    String email;
    String address;

    User(String email, String address) {
        this.email = email;
        this.address = address;
    }

    User(){

    }

    public User(String email) {
        this.email = email;
        this.address=null;
    }


    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("address", address);
        return result;
    }
}


