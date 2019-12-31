package com.bighugesystems.speechdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SpeechDemo";

    private SpeechRecognizer sr;
    private  TextToSpeech textToSpeech;

    private TextView textViewResult;
    private TextView textViewFlight;

    ProgressDialog prgDialog;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult = (TextView) findViewById(R.id.textResult);
        textViewFlight = (TextView) findViewById(R.id.textFlight);

        textToSpeech = new TextToSpeech(getApplicationContext(),null);

        mHandler = new Handler(Looper.getMainLooper());

        requestRecordAudioPermission();
    }
    // --------------------------------------------------------------------------------------------
    // Speech output
    private void speak(String textToSpeak){
        textToSpeech.setPitch(1.0f);
        textToSpeech.setSpeechRate(0.80f);
        int speechStatus = textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);

        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }

    // --------------------------------------------------------------------------------------------
    // Standard Speech Recognition showing Google Splash Screen
    public void listenStandard(View view) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Which flight are you looking for ?");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn not support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }

    // --------------------------------------------------------------------------------------------
    // Bouncing speech recognition of to Google Search
    public void listenWebSearch(View view) {
        Intent i = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(i, 101);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn not support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }

    // --------------------------------------------------------------------------------------------
    // Catch hardware button events
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.v(TAG, event.toString());
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode==KeyEvent.KEYCODE_MEDIA_PLAY){
            Log.i(TAG, event.toString());
            listenHeadless();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // --------------------------------------------------------------------------------------------
    // Headless speech recognition implementation
    public void listenHeadless(View view){
        listenHeadless();
    }

    private void listenHeadless(){
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listener());

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,10);
        sr.startListening(intent);
        Log.i(TAG,"Start listening");
    }

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            //Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
                result.append(i + ":" + data.get(i) + "\n");
            }
            textViewResult.setText(result);
            //speak("You said: " + data.get(0));
            processCommand(data.get(0).toString());
        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Activity Results handling
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                StringBuffer result = new StringBuffer();

                for (int i = 0; i < res.size(); i++) {
                    Log.d(TAG, "result " + res.get(i));
                    result.append(i + ":" + res.get(i) + "\n");
                }
                textViewResult.setText(result);
            }
            else {
                textViewResult.setText("Error:" + resultCode);
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Android Permissions
    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Process spoken requests
    private void processCommand(String sttIN) {
        sttIN = sttIN.toLowerCase();
        String searchDate = "";
        String searchFlightNo = "";

        // Look for general date statements
        if (sttIN.contains("tomorrow")){
            searchDate = getTomorrowDateString();
        } else if (sttIN.contains("yesterday")){
            searchDate = getYesterdayDateString();
        } else {
            searchDate = getTodayDateString();
        }

        String line = sttIN;
        String pattern = "(flight)?\\s*(\\w{2})\\s*(\\d{1,4})";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);
        // Now create matcher object.
        Matcher m = r.matcher(line);
        if (m.find()) {
            searchFlightNo = m.group(3);
            System.out.println(m.toString());
        } else {
            System.out.println("NO MATCH");
        }

        System.out.println("Date:" + searchDate + " Flight:" +searchFlightNo);
        showProgress();
        searchFlight(searchDate,searchFlightNo);
    }

    private void showProgress() {
        prgDialog = new ProgressDialog(this);
        prgDialog.setMessage("Please wait...");
        prgDialog.setCancelable(false);
        prgDialog.show();
    }

    private String getTodayDateString(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    private String getYesterdayDateString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    private String getTomorrowDateString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        return dateFormat.format(cal.getTime());
    }

    // --------------------------------------------------------------------------------------------
    // IAG Webservice call
    private void searchFlight(String flightDate, String flightNumber) {

        OkHttpClient httpClient = new OkHttpClient();

        String callURL = "https://api.ba.com/rest-v1/v1/flights;scheduledDepartureDate=" + flightDate + ";flightNumber="+ flightNumber;
        System.out.println(callURL);

        Request request = new Request.Builder().url(callURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("client-key", "YOURCODE")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("WS Call failed (1):" + e.getMessage());
                prgDialog.dismiss();
            }

            @Override
            public void onResponse(Call call, Response response) {
                ResponseBody responseBody = response.body();

                if (!response.isSuccessful()) {
                    final String errRep = "WS Call failed (2):" + response.toString();
                    final int errCode = response.code();
                    System.out.println(errCode);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            textViewFlight.setText(errRep);
                        }
                    });
                    prgDialog.dismiss();
                } else {
                    try {
                        String str = new String(response.body().string());
                        prgDialog.dismiss();
                        final JSONObject svcresponse = new JSONObject(str);
                        int spacesToIndentEachLevel = 2;
                        final String prettyPrintString = svcresponse.toString(spacesToIndentEachLevel);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                textViewFlight.setText(prettyPrintString);
                                speak("Response ok");
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        final String errStr = e.getMessage();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                               textViewFlight.setText(errStr);
                            }
                        });
                        prgDialog.dismiss();
                    }
                }
            }
        });

    }
}


//Photo by Jonathan Farber on Unsplash