package com.example.cameraapp;



import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
//import com.microsoft.projectoxford.face.FaceServiceClient;
//import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button btnTakePicture, btnProcess;

    EmotionServiceClient restClient = new EmotionServiceRestClient("EMOTIONKEY");
    int TAKE_PICTURE_CODE = 100, REQUEST_PERMISSION_CODE = 101;
    String SpotifyPlayList = "https://open.spotify.com/playlist/1htbVHQaC4b5HTtaogmK1Q?si=5Bpf5bEtSIG5fWD-hsC9wQ";
    String emotion = "Happiness";
    Bitmap mBitmap;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_CODE) {
            Toast.makeText(this, "Permission Granted",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission Denied",Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intiViews();
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET}, REQUEST_PERMISSION_CODE);
        }
    }

    private void intiViews() {
        btnProcess = (Button)findViewById(R.id.btnProcess);
        btnTakePicture = (Button)findViewById(R.id.btnTakePic);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setRotation((float) 90.0);




        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicFromGallery();
            }
        });
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processImage();
            }
        });
    }

    private void processImage() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream,String, List<RecognizeResult>> processAsync = new AsyncTask<InputStream, String, List<RecognizeResult>>() {

            ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

            @Override
            protected void onPreExecute() {
                mDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                mDialog.setMessage(values[0]);
            }

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... params) {
                publishProgress("Please Wait...");
                List<RecognizeResult> result = null;
                try {
                    result = restClient.recognizeImage(params[0]);
                } catch (EmotionServiceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
                mDialog.dismiss();
                if(recognizeResults != null) {
                    for (int i = 0; i < recognizeResults.size(); i++) {
                        String status = getEmotion(recognizeResults.get(i));
                        imageView.setImageBitmap(ImageHelper.drawRectOnBitmap(mBitmap, recognizeResults.get(i).faceRectangle, status));
                    }
                } else {
                    Log.e("Error", "RecognizeResults is null");
                }

                Uri uri = Uri.parse(SpotifyPlayList); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        };
        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult res) {
        List<Double> list = new ArrayList<>();
        Scores scores = res.scores;

        list.add(scores.anger);
        list.add(scores.happiness);
        list.add(scores.contempt);
        list.add(scores.disgust);
        list.add(scores.fear);
        list.add(scores.neutral);
        list.add(scores.sadness);
        list.add(scores.surprise);

        Collections.sort(list);
        double maxNum = list.get(list.size()-1);

        if(maxNum == scores.anger) {
            SpotifyPlayList = "https://open.spotify.com/playlist/4CEQdSIZDLnBjdWoSfzkvz?si=sdpfTIQZSDqbh5bkQGpT9Q";
            emotion = "Anger";
            return "anger";
        }
        else if(maxNum == scores.happiness) {
            SpotifyPlayList = "https://open.spotify.com/playlist/1htbVHQaC4b5HTtaogmK1Q?si=5Bpf5bEtSIG5fWD-hsC9wQ";
            emotion = "Happiness";
            return "happiness";
        }
        else if(maxNum == scores.contempt) {
            SpotifyPlayList = "https://open.spotify.com/playlist/3mgBz2U6pV5TXNN9xYAr14?si=gvbm00_7RleK3Fs7Kqk9rg";
            emotion = "Neutral";
            return "contempt";
        }
        else if(maxNum == scores.disgust) {
            SpotifyPlayList = "https://open.spotify.com/playlist/4CEQdSIZDLnBjdWoSfzkvz?si=sdpfTIQZSDqbh5bkQGpT9Q";
            emotion = "Anger";
            return "disgust";
        }
        else if(maxNum == scores.fear) {
            SpotifyPlayList = "https://open.spotify.com/playlist/16W9mnx6Soduv0nEhtBj8o?si=RpVlWFjcRcqlMekZUmoXdg";
            emotion = "Fear";
            return "fear";
        }
        else if(maxNum == scores.neutral) {
            SpotifyPlayList = "https://open.spotify.com/playlist/3mgBz2U6pV5TXNN9xYAr14?si=gvbm00_7RleK3Fs7Kqk9rg";
            emotion = "Neutral";
            return "neutral";
        }
        else if(maxNum == scores.surprise) {
            SpotifyPlayList = "https://open.spotify.com/playlist/16W9mnx6Soduv0nEhtBj8o?si=RpVlWFjcRcqlMekZUmoXdg";
            emotion = "Fear";
            return "surprise";
        } else {
            SpotifyPlayList = "https://open.spotify.com/playlist/6U6mC5Dg8UxCSpLKBGNBvm?si=P5xxC78LT-SoygOfHCVuDQ";
            emotion = "Sad";
            return "sadness";
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == TAKE_PICTURE_CODE) {
            Uri selectedImageUri = data.getData();
            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(selectedImageUri);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            mBitmap = BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(mBitmap);
        }
    }

    private void takePicFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,TAKE_PICTURE_CODE);
    }
}
