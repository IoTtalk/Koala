package cc.nctu1210.sample.koala6x;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectECActivity extends Activity {
	static final String local_tag = SelectECActivity.class.getSimpleName();
	
	final ArrayList<ECListItem> ec_endpoint_list = new ArrayList<ECListItem>();
    ArrayAdapter<ECListItem> adapter;
	final DAN.Subscriber event_subscriber = new EventSubscriber();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.logging(local_tag, "========== SelectECActivity start ==========");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_ec_list);

		DAN.set_log_tag(Constants.log_tag);
        DAN.init(event_subscriber);
        if (DAN.session_status()) {
        	Utils.logging(local_tag, "Already registered, jump to SessionActivity");
            Intent intent = new Intent(SelectECActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        
        adapter = new ECListAdapter(this, R.layout.item_ec_list, ec_endpoint_list);
        for (String i: DAN.available_ec()) {
            ec_endpoint_list.add(new ECListItem(i));
        }

        // show available EC ENDPOINTS
        final ListView lv_available_ec_endpoints = (ListView)findViewById(R.id.lv_available_ec_endpoints);
        lv_available_ec_endpoints.setAdapter(adapter);
        lv_available_ec_endpoints.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                    View view, int position, long id) {
            	ECListItem ec_list_item = ec_endpoint_list.get(position);
                next_step(ec_list_item.ec_endpoint);
    	        ec_list_item.status = ECListItem.Status.CONNECTING;
    	        adapter.notifyDataSetChanged();
    	        Utils.show_ec_status_on_notification(SelectECActivity.this, ec_list_item.ec_endpoint, false);
            }
        });

		final Button btn_register = (Button)findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et_endpoint = (EditText)findViewById(R.id.et_endpoint);
                next_step(et_endpoint.getText().toString());
            }
        });
    }

    void next_step(String endpoint) {
        DAN.unsubscribe(event_subscriber);
        Intent intent = new Intent(SelectECActivity.this, MainActivity.class);
        Bundle b = new Bundle();
        b.putString("endpoint", endpoint);
        intent.putExtras(b);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onPause () {
    	super.onPause();
		DAN.unsubscribe(event_subscriber);
    }
    
    class EventSubscriber implements DAN.Subscriber {
	    public void odf_handler (final String feature, final DAN.ODFObject odf_object) {
			if (!feature.equals(DAN.CONTROL_CHANNEL)) {
				Utils.logging(local_tag, "EventSubscriber should only receive {} events", DAN.CONTROL_CHANNEL);
				return;
			}
	    	switch (odf_object.event) {
			case FOUND_NEW_EC:
				Utils.logging(local_tag, "FOUND_NEW_EC: %s", odf_object.message);
				ec_endpoint_list.add(new ECListItem(odf_object.message));
    	    	runOnUiThread(new Thread () {
    	    		@Override
    	    		public void run () {
    	    			adapter.notifyDataSetChanged();
    	    		}
    	    	});
				break;
			case REGISTER_FAILED:
                Utils.logging("SelectECActivity", "REGISTER_FAILED");
                for (ECListItem ec_list_item: ec_endpoint_list) {
                    if (ec_list_item.ec_endpoint.equals(odf_object.message)) {
                        ec_list_item.status = ECListItem.Status.FAILED;
                    }
                }
				runOnUiThread(new Thread () {
    	    		@Override
    	    		public void run () {
                        ((Button) findViewById(R.id.btn_register)).setText("Register");
    					Toast.makeText(
    							getApplicationContext(),
    							"Register to "+ odf_object.message +" failed.",
    							Toast.LENGTH_LONG).show();
                        adapter.notifyDataSetChanged();
    	    		}
    	    	});
    	        Utils.remove_all_notification(SelectECActivity.this);
				break;
			default:
				break;
	    	}
	    }
	}
    
    public static class ECListItem {
        public enum Status {
            UNKNOWN,
            FAILED,
            CONNECTING,
            // no SUCCEED needed, because once successfully connected to an EC,
            //  there would be no reason to stay in this Activity
        }
    	public String ec_endpoint;
    	public Status status;
    	public ECListItem (String e) {
    		this.ec_endpoint = e;
    		this.status = Status.UNKNOWN;
    	}
    }

	public class ECListAdapter extends ArrayAdapter<ECListItem> {
	    Context context;
	    int layout_resource_id;
	    ArrayList<ECListItem> data = null;
	    
	    public ECListAdapter (Context context, int layout_resource_id, ArrayList<ECListItem> data) {
	        		super(context, layout_resource_id, data);
	        this.context = context;
	        this.layout_resource_id = layout_resource_id;
	        this.data = data;
	    }
	    
	    @Override
	    public View getView (int position, View convertView, ViewGroup parent) {
	        View row = convertView;
	        ECEndpointHolder holder = null;
	        
	        if(row == null) {
	            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
	            row = inflater.inflate(layout_resource_id, parent, false);
	            holder = new ECEndpointHolder();
	            holder.ec_endpoint = (TextView)row.findViewById(R.id.tv_ec_endpoint);
	            holder.connecting = (TextView)row.findViewById(R.id.tv_connecting);
	            row.setTag(holder);
	        } else {
	            holder = (ECEndpointHolder)row.getTag();
	        }
	        
	        ECListItem i = data.get(position);
	        holder.ec_endpoint.setText(i.ec_endpoint.replace("http://", "").replace(":9999", ""));
	        switch (i.status) {
                case UNKNOWN:
                    holder.connecting.setText("");
                    break;
                case FAILED:
                    holder.connecting.setText("x");
                    break;
                case CONNECTING:
                    holder.connecting.setText("...");
                    break;
            }
	        return row;
	    }
	
	    class ECEndpointHolder {
	        TextView ec_endpoint;
	        TextView connecting;
	    }
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, Constants.MENU_ITEM_ID_DAN_VERSION, 0, "DAN Version: "+ DAN.version);
        menu.add(0, Constants.MENU_ITEM_ID_DAI_VERSION, 0, "DAI Version: "+ Constants.version);
        menu.add(0, Constants.MENU_ITEM_WIFI_SSID, 0, "WiFi: "+ Utils.get_wifi_ssid(getApplicationContext()));
        return super.onPrepareOptionsMenu(menu);
    }
}
