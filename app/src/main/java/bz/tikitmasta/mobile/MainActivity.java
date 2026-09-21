package bz.tikitmasta.mobile;

import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.*;

public class MainActivity extends AppCompatActivity {
    LinearLayout content; SharedPreferences prefs; ApiClient api;
    final androidx.activity.result.ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(new ScanContract(), result -> { if(result.getContents()!=null) submitScan(result.getContents()); });
    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("tikit",MODE_PRIVATE); showHome(); }
    TextView title(String s,int size){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(35,25,55)); v.setPadding(0,12,0,12); return v; }
    MaterialButton button(String text,View.OnClickListener l){ MaterialButton b=new MaterialButton(this); b.setText(text); b.setOnClickListener(l); b.setAllCaps(false); b.setTextSize(16); return b; }
    void page(String heading){ ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(28,42,28,42); sc.addView(content); content.addView(title("TIKIT MASTA",25)); content.addView(title(heading,21)); setContentView(sc); }
    String server(){ return prefs.getString("server","https://your-odoo-domain.com"); }
    String token(){ return prefs.getString("device_token",""); }
    void initApi(){ api=new ApiClient(server()); }
    void showHome(){ page("Events & Gate App"); content.addView(title("Browse live events, sell at the gate, and validate tickets securely against Odoo 18.",16)); content.addView(button("Browse Events",v->events())); content.addView(button("Scan Ticket",v->scan())); content.addView(button("Gate Sale",v->gateSale())); content.addView(button("Promoter Dashboard",v->dashboard())); content.addView(button("Settings",v->settings())); }
    void back(){ content.addView(button("Back",v->showHome())); }
    void settings(){ page("Connection Settings"); TextInputEditText s=new TextInputEditText(this); s.setHint("Odoo URL"); s.setText(server()); content.addView(s); TextInputEditText t=new TextInputEditText(this); t.setHint("Scanner device token"); t.setText(token()); content.addView(t); content.addView(button("Save & Test",v->{ prefs.edit().putString("server",s.getText().toString().trim()).putString("device_token",t.getText().toString().trim()).apply(); initApi(); api.post("/bz_eventpass/api/v1/scanner/status",new JSONObjectBuilder().put("device_token",token()).build(),(r,e)->runOnUiThread(()->toast(e==null&&r.optBoolean("ok")?"Connected":"Saved. "+(e!=null?e:r.optString("error"))))); })); back(); }
    void events(){ page("Upcoming Events"); initApi(); TextView status=title("Loading…",16); content.addView(status); back(); api.post("/bz_eventpass/api/v1/events",new JSONObject(),(r,e)->runOnUiThread(()->{ status.setText(""); if(e!=null||r==null||!r.optBoolean("ok")){status.setText(e!=null?e:r.optString("error"));return;} JSONArray a=r.optJSONArray("events"); for(int i=0;a!=null&&i<a.length();i++){ JSONObject x=a.optJSONObject(i); LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(18,18,18,18); card.addView(title(x.optString("name"),19)); card.addView(title(x.optString("start_datetime")+"\n"+x.optString("venue_name")+", "+x.optString("city"),14)); card.addView(button("View / Buy Tickets",v->startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(api.absolute(x.optString("public_url"))))))); content.addView(card,content.getChildCount()-1); } })); }
    void scan(){ if(token().isEmpty()){toast("Add the scanner device token in Settings first");settings();return;} ScanOptions o=new ScanOptions(); o.setPrompt("Scan a Tikit Masta QR ticket"); o.setBeepEnabled(true); o.setOrientationLocked(true); scanner.launch(o); }
    void submitScan(String value){ page("Checking Ticket"); TextView s=title("Validating…",18);content.addView(s); initApi(); api.post("/bz_eventpass/api/v1/ticket/scan",new JSONObjectBuilder().put("device_token",token()).put("scan_value",value).build(),(r,e)->runOnUiThread(()->{ boolean ok=e==null&&r!=null&&r.optBoolean("ok"); s.setText(ok?"✓ ACCEPTED\n"+ticketSummary(r.optJSONObject("ticket")):"✕ NOT ACCEPTED\n"+(e!=null?e:r.optString("error"))); s.setTextColor(ok?Color.rgb(0,125,70):Color.rgb(190,30,45)); content.addView(button("Scan Next Ticket",v->scan())); back(); })); }
    String ticketSummary(JSONObject t){ return t==null?"Ticket checked in":t.optString("customer_name")+"\n"+t.optString("event_name")+"\n"+t.optString("ticket_type_name"); }
    void gateSale(){ page("Gate Sale"); TextInputEditText event=field("Event ID"),type=field("Ticket Type ID"),qty=field("Quantity (default 1)"),name=field("Customer name"),email=field("Customer email"),phone=field("Customer phone"),method=field("Payment method: cash/card/wallet"); content.addView(button("Complete Sale",v->{ initApi(); JSONObject p=new JSONObjectBuilder().put("device_token",token()).put("event_id",event.getText()).put("ticket_type_id",type.getText()).put("quantity",qty.getText().length()==0?1:qty.getText()).put("customer_name",name.getText()).put("customer_email",email.getText()).put("customer_phone",phone.getText()).put("payment_method",method.getText().length()==0?"cash":method.getText()).build(); api.post("/bz_eventpass/api/v1/gate/sale",p,(r,e)->runOnUiThread(()->toast(e!=null?e:(r.optBoolean("ok")?"Sale completed: "+r.optJSONObject("gate_sale").optString("receipt_number"):r.optString("error"))))); })); back(); }
    TextInputEditText field(String hint){ TextInputEditText f=new TextInputEditText(this);f.setHint(hint);content.addView(f);return f; }
    void dashboard(){ page("Promoter Dashboard"); TextInputEditText account=field("Promoter account number"),key=field("API token"); content.addView(button("Load Dashboard",v->{initApi();api.post("/bz_eventpass/api/v1/promoter/dashboard",new JSONObjectBuilder().put("account_number",account.getText()).put("api_token",key.getText()).build(),(r,e)->runOnUiThread(()->{if(e!=null||!r.optBoolean("ok")){toast(e!=null?e:r.optString("error"));return;}JSONObject p=r.optJSONObject("promoter");content.addView(title(p.optString("name")+"\nPlan: "+p.optString("business_plan")+"\nTickets sold: "+p.optInt("tickets_sold")+"\nRevenue: "+p.optDouble("attributed_revenue")+" "+p.optString("currency")+"\nCommission due: "+p.optDouble("commission_due"),17),content.getChildCount()-1);}));})); back(); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    static class JSONObjectBuilder { JSONObject o=new JSONObject(); JSONObjectBuilder put(String k,Object v){try{o.put(k,String.valueOf(v));}catch(Exception ignored){}return this;} JSONObject build(){return o;} }
}
