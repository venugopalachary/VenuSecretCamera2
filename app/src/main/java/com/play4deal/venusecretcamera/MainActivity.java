package com.play4deal.venusecretcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Accessory;
import com.microsoft.projectoxford.face.contract.Emotion;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FacialHair;
import com.microsoft.projectoxford.face.contract.Hair;
import com.microsoft.projectoxford.face.contract.HeadPose;
import com.microsoft.projectoxford.face.contract.Makeup;
import com.play4deal.venusecretcamera.listeners.PictureCapturingListener;
import com.play4deal.venusecretcamera.services.APictureCapturingService;
import com.play4deal.venusecretcamera.services.PictureCapturingServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity  implements PictureCapturingListener, ActivityCompat.OnRequestPermissionsResultCallback {


    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    private ImageView uploadBackPhoto;

     private Bitmap scaled;
    //The capture service
    private APictureCapturingService pictureService;
     PictureCapturingServiceImpl pictureCapturingServiceimpl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();

        uploadBackPhoto = (ImageView) findViewById(R.id.backIV);
        final Button btn = (Button) findViewById(R.id.startCaptureBtn);
        // getting instance of the Service from PictureCapturingServiceImpl
        pictureService = PictureCapturingServiceImpl.getInstance(this);



        btn.setOnClickListener(v -> {
            showToast("Starting capture!");
            pictureService.startCapturing(this);
        });


    }

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }


    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {

        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                 Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                  scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);

                Toast.makeText(getBaseContext(),"venujij"+scaled,Toast.LENGTH_LONG).show();

                if (pictureUrl.contains("0_pic.jpg")) {

                    uploadBackPhoto.setImageBitmap(scaled);


                } else if (pictureUrl.contains("1_pic.jpg")) {
                    //uploadFrontPhoto.setImageBitmap(scaled);
                }
            });
            showToast("Picture saved to " + pictureUrl);
        }

    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {

        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

         //   pictureCapturingService.stopCapturing(this);
            // Start a background task to detect faces in the image.
            new DetectionTask().execute(inputStream);
            return;
        }
        showToast("No camera detected!");

    }


    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }


    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender

                        });
            } catch (Exception e) {
                mSucceed = false;
               // publishProgress(e.getMessage());
             //   addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
          //  mProgressDialog.show();
          //  addLog("Request: Detecting in image " + mImageUri);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
          //  mProgressDialog.setMessage(progress[0]);
          //  setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
              //  addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                //        + " face(s) in " + mImageUri);
            }
            Toast.makeText(getBaseContext(),result+"venu",Toast.LENGTH_LONG).show();
            Log.i("venuresponse",result+"");
            // Show the result on screen when detection is done.
          //  setUiAfterDetection(result, mSucceed);
        }
    }




    // The adapter of the GridView which contains the details of the detected faces.
    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result.
        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    // Crop face thumbnail with five main landmarks drawn from original image.
                    // faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                    //    mBitmap, face.faceRectangle));
                }
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            // Show the face details.
            DecimalFormat formatter = new DecimalFormat("#0.0");
            String face_description = String.format("Age: %s  Gender: %s\nHair: %s  FacialHair: %s\nMakeup: %s  %s\nForeheadOccluded: %s  Blur: %s\nEyeOccluded: %s  %s\n" +
                            "MouthOccluded: %s  Noise: %s\nGlassesType: %s\nHeadPose: %s\nAccessories: %s",
                    faces.get(position).faceAttributes.age,
                    faces.get(position).faceAttributes.gender,
                    getHair(faces.get(position).faceAttributes.hair),
                    getFacialHair(faces.get(position).faceAttributes.facialHair),
                    getMakeup((faces.get(position)).faceAttributes.makeup),
                    getEmotion(faces.get(position).faceAttributes.emotion),
                    faces.get(position).faceAttributes.occlusion.foreheadOccluded,
                    faces.get(position).faceAttributes.blur.blurLevel,
                    faces.get(position).faceAttributes.occlusion.eyeOccluded,
                    faces.get(position).faceAttributes.exposure.exposureLevel,
                    faces.get(position).faceAttributes.occlusion.mouthOccluded,
                    faces.get(position).faceAttributes.noise.noiseLevel,
                    faces.get(position).faceAttributes.glasses,
                    getHeadPose(faces.get(position).faceAttributes.headPose),
                    getAccessories(faces.get(position).faceAttributes.accessories)
            );
            ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(face_description);

            Toast.makeText(getApplicationContext(),face_description,Toast.LENGTH_LONG).show();

            return convertView;
        }

        private String getHair(Hair hair) {
            if (hair.hairColor.length == 0)
            {
                if (hair.invisible)
                    return "Invisible";
                else
                    return "Bald";
            }
            else
            {
                int maxConfidenceIndex = 0;
                double maxConfidence = 0.0;

                for (int i = 0; i < hair.hairColor.length; ++i)
                {
                    if (hair.hairColor[i].confidence > maxConfidence)
                    {
                        maxConfidence = hair.hairColor[i].confidence;
                        maxConfidenceIndex = i;
                    }
                }

                return hair.hairColor[maxConfidenceIndex].color.toString();
            }
        }

        private String getMakeup(Makeup makeup) {
            return  (makeup.eyeMakeup || makeup.lipMakeup) ? "Yes" : "No" ;
        }

        private String getAccessories(Accessory[] accessories) {
            if (accessories.length == 0)
            {
                return "NoAccessories";
            }
            else
            {
                String[] accessoriesList = new String[accessories.length];
                for (int i = 0; i < accessories.length; ++i)
                {
                    accessoriesList[i] = accessories[i].type.toString();
                }

                return TextUtils.join(",", accessoriesList);
            }
        }

        private String getFacialHair(FacialHair facialHair) {
            return (facialHair.moustache + facialHair.beard + facialHair.sideburns > 0) ? "Yes" : "No";
        }

        private String getEmotion(Emotion emotion)
        {
            String emotionType = "";
            double emotionValue = 0.0;
            if (emotion.anger > emotionValue)
            {
                emotionValue = emotion.anger;
                emotionType = "Anger";
            }
            if (emotion.contempt > emotionValue)
            {
                emotionValue = emotion.contempt;
                emotionType = "Contempt";
            }
            if (emotion.disgust > emotionValue)
            {
                emotionValue = emotion.disgust;
                emotionType = "Disgust";
            }
            if (emotion.fear > emotionValue)
            {
                emotionValue = emotion.fear;
                emotionType = "Fear";
            }
            if (emotion.happiness > emotionValue)
            {
                emotionValue = emotion.happiness;
                emotionType = "Happiness";
            }
            if (emotion.neutral > emotionValue)
            {
                emotionValue = emotion.neutral;
                emotionType = "Neutral";
            }
            if (emotion.sadness > emotionValue)
            {
                emotionValue = emotion.sadness;
                emotionType = "Sadness";
            }
            if (emotion.surprise > emotionValue)
            {
                emotionValue = emotion.surprise;
                emotionType = "Surprise";
            }
            return String.format("%s: %f", emotionType, emotionValue);
        }

        private String getHeadPose(HeadPose headPose)
        {
            return String.format("Pitch: %s, Roll: %s, Yaw: %s", headPose.pitch, headPose.roll, headPose.yaw);
        }
    }

    // Show the result on screen when detection is done.
    private void setUiAfterDetection(Face[] result, boolean succeed) {
        // Detection is done, hide the progress dialog.
      //  mProgressDialog.dismiss();

        // Enable all the buttons.
       // setAllButtonsEnabledStatus(true);

        // Disable button "detect" as the image has already been detected.
      //  setDetectButtonEnabledStatus(false);

        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null) {
                detectionResult = result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected";

                // Show the detected faces on original image.
          //      ImageView imageView = (ImageView) findViewById(R.id.image);
        //        imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(
          //              mBitmap, result, true));

                // Set the adapter of the ListView which contains the details of the detected faces.
                Toast.makeText(getBaseContext(),result.toString(),Toast.LENGTH_LONG).show();
                FaceListAdapter faceListAdapter = new FaceListAdapter(result);

                // Show the detailed list of detected faces.
                ListView listView = (ListView) findViewById(R.id.list_detected_faces);
                listView.setAdapter(faceListAdapter);
            } else {
                detectionResult = "0 face detected";
            }
          //  setInfo(detectionResult);
        }

      //  mImageUri = null;
     //   mBitmap = null;
    }

}
