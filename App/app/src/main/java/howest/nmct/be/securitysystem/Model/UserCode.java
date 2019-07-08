package howest.nmct.be.securitysystem.Model;

/**
 * Created by Verschuere on 16-Dec-16.
 */
public class UserCode {

    public UserCode(){

    }

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("userId")
    private String mUserId;

    @com.google.gson.annotations.SerializedName("loginCodeId")
    private String mPersonId;
    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    /**
     * Indicates if the item is completed
     */




    /**
     * Initializes a new ToDoItem
     *
     * @param UserId
     *            The item text
     * @param id
     *            The item id
     */
    public UserCode(String UserId, String PersonId, String id) {
        this.setUserId(UserId);
        this.setPersonId(PersonId);
        this.setId(id);
    }

    /**
     * Returns the item text
     */
    public String GetUserId() {
        return mUserId;
    }

    /**
     * Sets the item text
     *
     * @param Code
     *            text to set
     */
    public final void setUserId(String Code) {
        mUserId = Code;
    }
    /**
     * Returns the item text
     */
    public String GetPersonId() {
        return mPersonId;
    }

    /**
     * Sets the item text
     *
     * @param Code
     *            text to set
     */
    public final void setPersonId(String Code) {
        mPersonId = Code;
    }

    /**
     * Returns the item id
     */
    public String getId() {
        return mId;
    }

    /**
     * Sets the item id
     *
     * @param id
     *            id to set
     */
    public final void setId(String id) {
        mId = id;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof UserCode && ((UserCode) o).mId == mId;
    }
}
