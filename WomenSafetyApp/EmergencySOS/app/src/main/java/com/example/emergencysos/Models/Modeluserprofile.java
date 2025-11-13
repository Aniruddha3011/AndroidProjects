package com.example.emergencysos.Models;

public class Modeluserprofile {
    String Fname;
    String Lname;
    String Age;
    String Mobno;
    String profileImageUrl;
    String Emergency_contact;
    String HouseNo;
    String landmark;
    String Road;
    String Pincode;
    String state;
    String city;

    public Modeluserprofile(String fname, String lname, String age, String mobno, String emergency_contact,
                            String houseNo, String landmark, String road, String pincode, String city, String state,
                            String profileImageUrl) {
        Fname = fname;
        Lname = lname;
        Age = age;
        Mobno = mobno;
        Emergency_contact = emergency_contact;
        HouseNo = houseNo;
        this.landmark = landmark;
        Road = road;
        Pincode = pincode;
        this.city = city;
        this.state = state;
        this.profileImageUrl = profileImageUrl;
    }


    public String getFname() {
        return Fname;
    }

    public void setFname(String fname) {
        Fname = fname;
    }

    public String getLname() {
        return Lname;
    }

    public void setLname(String lname) {
        Lname = lname;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String age) {
        Age = age;
    }

    public String getMobno() {
        return Mobno;
    }

    public void setMobno(String mobno) {
        Mobno = mobno;
    }

    public String getEmergency_contact() {
        return Emergency_contact;
    }

    public void setEmergency_contact(String emergency_contact) {
        Emergency_contact = emergency_contact;
    }

    public String getHouseNo() {
        return HouseNo;
    }

    public void setHouseNo(String houseNo) {
        HouseNo = houseNo;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getRoad() {
        return Road;
    }

    public void setRoad(String road) {
        Road = road;
    }

    public String getPincode() {
        return Pincode;
    }

    public void setPincode(String pincode) {
        Pincode = pincode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
