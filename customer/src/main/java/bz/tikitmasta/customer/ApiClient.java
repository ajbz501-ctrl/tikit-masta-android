package bz.tikitmasta.customer;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public interface Callback { void done(JSONObject result, String error, int status); }
    public static final String BASE = "https://tikitmasta.com";
    private String accessToken = "";
    public void setAccessToken(String token) { accessToken = token == null ? "" : token; }
    public String absolute(String path) { return path.startsWith("http") ? path : BASE + (path.startsWith("/") ? path : "/" + path); }
    public void get(String path, Callback cb) { request("GET", path, null, false, cb); }
    public void patch(String path, JSONObject body, Callback cb) { request("PATCH", path, body, false, cb); }
    public void post(String path, JSONObject body, Callback cb) { request("POST", path, body, false, cb); }
    public void rpc(String path, JSONObject params, Callback cb) { request("POST", path, params, true, cb); }
    private void request(String method, String path, JSONObject input, boolean rpc, Callback cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection)new URL(absolute(path)).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(25000); c.setRequestMethod(method);
                c.setRequestProperty("Accept", "application/json"); c.setRequestProperty("Content-Type", "application/json");
                if (!accessToken.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + accessToken);
                if (input != null) {
                    c.setDoOutput(true);
                    JSONObject body = rpc ? new JSONObject().put("jsonrpc","2.0").put("method","call").put("params",input).put("id",System.currentTimeMillis()) : input;
                    try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                }
                int status=c.getResponseCode(); InputStream in=status<400?c.getInputStream():c.getErrorStream();
                StringBuilder s=new StringBuilder(); if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){for(String line;(line=r.readLine())!=null;)s.append(line);}
                JSONObject env=s.length()==0?new JSONObject():new JSONObject(s.toString());
                if(rpc){if(env.has("error"))cb.done(null,env.optJSONObject("error").optString("message","Server error"),status);else cb.done(env.optJSONObject("result"),null,status);}
                else cb.done(env,env.optBoolean("ok",status<400)?null:env.optString("error","Request failed"),status);
            } catch(Exception e){cb.done(null,e.getMessage(),0);} finally{if(c!=null)c.disconnect();}
        }).start();
    }
}
