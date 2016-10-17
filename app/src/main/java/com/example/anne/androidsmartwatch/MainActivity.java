package com.example.anne.androidsmartwatch;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

    Object mSpeechRecognizer;
    private TextView text1;
    private TextView text2;
    private TextView ack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        text1 = (TextView) findViewById(R.id.textView1);
        text2 = (TextView) findViewById(R.id.textView3);
        ack = (TextView) findViewById(R.id.Acknowledge);

        //Create SpeechRecognizer

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        ((SpeechRecognizer) mSpeechRecognizer).setRecognitionListener(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button:
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

                        //Start Recognizing

                        ((SpeechRecognizer) mSpeechRecognizer).startListening(intent);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Toast.makeText(this, "Ready to Record", Toast.LENGTH_SHORT);
        text1.setText("Ready!");
    }

    @Override
    public void onBeginningOfSpeech() {
        Toast.makeText(this, "Start Recording", Toast.LENGTH_SHORT);
        text1.setText("Start!");
    }

    @Override
    public void onEndOfSpeech() {
        Toast.makeText(this, "End Recording", Toast.LENGTH_SHORT);
        text1.setText("End!");
    }

    //System Data Record
    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.v("DEBUG","onBufferReceived");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.v("DEBUG","receive : " + rmsdB + "dB");
    }

    //Error
    @Override
    public void onError(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                Toast.makeText(this, "Fail to save audio", Toast.LENGTH_LONG);
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                Toast.makeText(this, "Android Client Error", Toast.LENGTH_LONG);
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Toast.makeText(this, "Fail to Match", Toast.LENGTH_LONG);
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Toast.makeText(this, "No Input!", Toast.LENGTH_LONG);
                break;
            default:
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.v("DEBUG","onEvent");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.v("DEBUG","onPartialResults");
    }

    //Return Results
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> recData = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        final String[] str_items = recData.toArray(new String[recData.size()]);
        String SendResult = str_items[0];
        text2.setText(SendResult);
        new SendHuzzah().execute(SendResult);
    }


    //Start Network Service

    class SendHuzzah extends AsyncTask<String, Void, String> {
        /**
         * The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute()
         */
        protected String doInBackground(String...command) {
            String[] SendCommand = command;

            String AckResult;

            String url = "http://056cbcde.ngrok.io"; //Need to be updated when Huzzah connected to WiFi

            JSONObject comm = new JSONObject();

            try {
                URL obj = new URL(url);
                HttpURLConnection http = (HttpURLConnection) obj.openConnection();
                http.setConnectTimeout(20000);
                http.setDoInput(true);
                http.setDoOutput(true);
                http.setRequestMethod("POST");
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Host","056cbcde.ngrok.io");


                comm.put("api_command", SendCommand[0]);

                OutputStream os = http.getOutputStream();
                os.write(comm.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
                http.connect();

                int responseCode = http.getResponseCode();
                System.out.println("Response Code : " + responseCode);
                StringBuilder response = new StringBuilder();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(http.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    AckResult = "Success";

                } else {
                    AckResult = "Error";
                }

                return AckResult;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(String AckResult) {
            ack.append(AckResult);

        }
    }

}
