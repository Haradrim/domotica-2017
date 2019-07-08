package howest.nmct.be.securitysystem.Model;

/**
 * Created by Verschuere on 16-Dec-16.
 */
public class LoginCode {

    public LoginCode(){

    }

    /**
     * Item text
     */
    @com.google.gson.annotations.SerializedName("code")
    private int mCode;

    @com.google.gson.annotations.SerializedName("targetEmail")
    private String Email;

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    /**
     * Indicates if the item is completed
     */


    @Override
    public String toString() {
        return "" +  GetCode();
    }

    /**
     * Initializes a new ToDoItem
     *
     * @param Code
     *            The item text
     * @param id
     *            The item id
     */
    public LoginCode(String email,int Code, String id) {
        this.setCode(Code);
        this.setEmail(email);
        this.setId(id);
    }

    /**
     * Returns the item text
     */
    public int GetCode() {
        return mCode;
    }

    /**
     * Sets the item text
     *
     * @param Code
     *            text to set
     */
    public final void setCode(int Code) {
        mCode = Code;
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
        return o instanceof LoginCode && ((LoginCode) o).mId == mId;
    }
}
