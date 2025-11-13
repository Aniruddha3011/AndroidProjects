package com.example.emergencysos.Screens;

public class UserDataHolder {
    private static final UserDataHolder instance = new UserDataHolder();

    public String fname, lname, mobno, age, emergencyContact;
    public String houseNo, road, landmark, pincode, city, state;
    public String profileImageUrl;

    private UserDataHolder() {}

    public static UserDataHolder getInstance() {
        return instance;
    }
}
