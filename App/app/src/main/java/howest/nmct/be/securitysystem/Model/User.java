package howest.nmct.be.securitysystem.Model;


public class User {

    public User(){

    }

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("displayName")
    private String mDisplayName;
    @com.google.gson.annotations.SerializedName("personGivenName")
    private String mPersonGivenName;
    @com.google.gson.annotations.SerializedName("personFamilyName")
    private String mPersonFamilyName;
    @com.google.gson.annotations.SerializedName("personEmail")
    private String mpersonEmail;
    @com.google.gson.annotations.SerializedName("personId")
    private String mpersonId;
    @com.google.gson.annotations.SerializedName("id")
    private String mId;


    public User(String DisplayName, String PersonFamilyName, String PersonGivenName, String personEmail, String personId, String Id) {
      this.setDisplayName(DisplayName);
        this.setPersonId(personId);
        this.setPersonGivenName(PersonGivenName);
        this.setPersonFamilyName(PersonFamilyName);
        this.setId(Id);
        this.setPersonEmail(personEmail);
    }


    public String getPersonFamilyName() {
        return mPersonFamilyName;
    }

    public final void setPersonFamilyName(String PersonFamilyName) {
        mPersonFamilyName = PersonFamilyName;
    }

    public String getPersonId() {
        return mpersonId;
    }

    public final void setPersonId(String personId) {
        mpersonId = personId;
    }

    public String getPersonEmail() {
        return mpersonEmail;
    }

    public final void setPersonEmail(String personEmail) {
        mpersonEmail = personEmail;
    }

    public String getPersonGivenName() {
        return mPersonGivenName;
    }

    public final void setPersonGivenName(String PersonGivenName) {
        mPersonGivenName = PersonGivenName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public final void setDisplayName(String DisplayName) {
        mDisplayName = DisplayName;
    }



    public String getId() {
        return mId;
    }

    public final void setId(String id) {
        mId = id;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof User && ((User) o).mId == mId;
    }
}
