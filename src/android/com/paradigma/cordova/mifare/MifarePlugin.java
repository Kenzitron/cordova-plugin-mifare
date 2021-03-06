package com.paradigma.cordova.mifare;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.rfid.reader.Reader;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.concurrent.*;
import android.util.Log;

public class MifarePlugin extends CordovaPlugin {

    public Reader reader = null;
    private static final String TAG = "MifarePlugin";
    private static final Integer TIMEOUT_SECONDS_DEFAULT = 10;
    private static final String DEVICE = "/dev/ttyS3"; // UART serial port
    private Future<String> future = null;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("readUID")) {

            // Configure timeout
            final Integer timeout = args.length() != 0 ? args.getJSONObject(0).getInt("timeout") : TIMEOUT_SECONDS_DEFAULT;

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ExecutorService readUIDExecutor = Executors.newSingleThreadExecutor();
                    future = readUIDExecutor.submit(new Callable() {

                        public String call() throws Exception {
                            String uid = getUID();
                            PluginResult result = new PluginResult(PluginResult.Status.OK, uid);
                            result.setKeepCallback(true);
                            callbackContext.sendPluginResult(result);
                            return uid ;
                        }
                    });
                    try {
                        Log.d(TAG, "Start listening to Mifare Card...");
                        future.get(timeout, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        Log.d(TAG, "Mifare timeout");
                        JSONObject resultMessage = null;
                        try {
                            resultMessage = new JSONObject("{'code': 1, 'message': 'Mifare timeout (" + timeout + " seconds)' }");
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, resultMessage);
                        callbackContext.sendPluginResult(result);
                    } catch (Exception e) {
                        if (!future.isCancelled()){
                            JSONObject resultMessage = null;
                            try {
                                resultMessage = new JSONObject("{'code': 2 , 'message': 'Mifare error: " +  e.getMessage() + "'");
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Mifare error");
                            callbackContext.sendPluginResult(result);
                            Log.d(TAG, "Mifare error: " + e.getMessage());
                        }
                    } finally {
                        readUIDExecutor.shutdownNow();
                        future = null;
                    }
                }
            });
            return true;
        } else if (action.equals("closeReadUID")) {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                future.cancel(true);
                PluginResult result = new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(result);
            }else{
                JSONObject resultMessage = new JSONObject("{'code': 3, 'message': 'Mifare ReadUID was not initialized' }");
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, resultMessage);
                callbackContext.sendPluginResult(result);
            }

            return true;

        } else {
            return false;

        }
    }

    private Reader getReader() throws SecurityException, IOException, InvalidParameterException {
        if (reader == null) {
            reader = Reader.getInstance(DEVICE, 9600);
        }

        return reader;
    }

    private String getUID() throws IOException,SecurityException,InvalidParameterException{
        byte[] uid = new byte[32];
        byte[] uidLen = new byte[1];
        byte[] errCode = new byte[1];

        // Read until get response.
        int result = getReader().Iso14443a_GetUid(uid, uidLen, errCode);
        while (result != 0){
            result = getReader().Iso14443a_GetUid(uid, uidLen, errCode);
        }

        if (result != 0) {
            return "GetUid Error, errCode=" + String.format("%02X", (byte) errCode[0]);
        } else {
            String strUid = "";
            for (int i = 0; i < uidLen[0]; i++) {
                strUid += String.format("%02X ", uid[i]);
            }
            return strUid;
        }
    }
}