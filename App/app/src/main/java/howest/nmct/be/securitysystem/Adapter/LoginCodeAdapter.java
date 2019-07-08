package howest.nmct.be.securitysystem.Adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import howest.nmct.be.securitysystem.Model.LoginCode;
import howest.nmct.be.securitysystem.R;

public class LoginCodeAdapter extends ArrayAdapter<LoginCode> {

    Context mContext;

    /**
     * Adapter View layout
     */
    int mLayoutResourceId;


    public LoginCodeAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;
    }

    /**
     * Returns the view for a specific item on the list
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        final LoginCode currentItem = getItem(position);

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
        }

        row.setTag(currentItem);

        final TextView codetext = (TextView) row.findViewById(R.id.codeText);
        final TextView email = (TextView) row.findViewById(R.id.textviewEmailCode);
        codetext.setText("" + currentItem.GetCode());


       email.setText("" + currentItem.getEmail());


        return row;
    }

}
