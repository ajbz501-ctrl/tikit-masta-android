package bz.tikitmasta.mobile;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public interface Callback { void done(JSONObject result, String error); }
    private final String base;
    private String accessToken = "";
    public ApiClient(String base) { this.base = base.replaceAll("/+$", ""); }
    public void setAccessToken(String token) { accessToken = token == null ? "" : token; }
    public String absolute(String path) { return path.startsWith("http") ? path : base + (path.startsWith("/") ? path : "/" + path); }
    public void get(String path, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection)new URL(absolute(path)).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestMethod("GET");
                c.setRequestProperty("Accept", "application/json"); if(!accessToken.isEmpty()) c.setRequestProperty("Authorization","Bearer "+accessToken);
                InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
                StringBuilder s = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { for (String l; (l=r.readLine())!=null;) s.append(l); }
                JSONObject result = new JSONObject(s.toString());
                cb.done(result, result.optBoolean("ok") ? null : result.optString("error", "API unavailable"));
            } catch (Exception e) { cb.done(null, e.getMessage()); } finally { if (c != null) c.disconnect(); }
        }).start();
    }
    public void postV2(String path, JSONObject body, Callback cb) { raw("POST",path,body,cb); }
    public void getV2(String path, Callback cb) { raw("GET",path,null,cb); }
    private void raw(String method,String path,JSONObject body,Callback cb){new Thread(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(absolute(path)).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json");if(!accessToken.isEmpty())c.setRequestProperty("Authorization","Bearer "+accessToken);if(body!=null){c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}}InputStream in=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();StringBuilder s=new StringBuilder();if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){for(String l;(l=r.readLine())!=null;)s.append(l);}JSONObject out=s.length()==0?new JSONObject():new JSONObject(s.toString());cb.done(out,out.optBoolean("ok",false)?null:out.optString("error","Request failed"));}catch(Exception e){cb.done(null,e.getMessage());}finally{if(c!=null)c.disconnect();}}).start();}
    public void post(String path, JSONObject params, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection)new URL(absolute(path)).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestMethod("POST"); c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Accept", "application/json");
                JSONObject body = new JSONObject().put("jsonrpc","2.0").put("method","call").put("params",params).put("id",System.currentTimeMillis());
                try(OutputStream os=c.getOutputStream()){ os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
                StringBuilder s=new StringBuilder(); try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){ for(String l;(l=r.readLine())!=null;)s.append(l); }
                JSONObject envelope=new JSONObject(s.toString());
                if(envelope.has("error")) cb.done(null,envelope.getJSONObject("error").optString("message","Server error"));
                else cb.done(envelope.optJSONObject("result"),null);
            } catch(Exception e){ cb.done(null,e.getMessage()); } finally { if(c!=null)c.disconnect(); }
        }).start();
    }
}
