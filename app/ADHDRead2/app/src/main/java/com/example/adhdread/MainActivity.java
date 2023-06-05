package com.example.adhdread;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Uri photoUri;
    private TextView basicTextView;
    private TextView editedTextView;
    private File tempImageFile;
    private Button editedTextViewButton;
    private Button basicTextViewButton;
    private ActivityResultLauncher<Intent> imageCaptureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Find all buttons and textviews
        editedTextView = findViewById(R.id.textView2);
        basicTextView = findViewById(R.id.textView);
        basicTextViewButton = findViewById(R.id.button3);
        editedTextViewButton = findViewById(R.id.button4);

        // After image has been taken
        imageCaptureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) { // Check the result
                        if (tempImageFile != null) {
                            Bitmap bitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath()); // Get the bitmap
                            try {
                                bitmap = rotateImageIfRequired(bitmap); // Rotate
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String textFromPicture = extractTextFromImage(bitmap); // Get the text from the picture
                            basicTextView.setText(textFromPicture); // Set the normal text in the textview
                            editedTextView.setText(Html.fromHtml(FormatWords(textFromPicture))); // Set the adhd text in the textview
                            editedTextView.setVisibility(View.INVISIBLE);
                        }
                    }
                });
        // Add listeners to change visibilities when the adhd text or normal text button is clicked
        editedTextViewButton.setOnClickListener(v -> {
            editedTextView.setVisibility(View.VISIBLE);
            basicTextView.setVisibility(View.INVISIBLE);
        });
        basicTextViewButton.setOnClickListener( v -> {
            basicTextView.setVisibility(View.VISIBLE);
            editedTextView.setVisibility(View.INVISIBLE);
        });
        // Run the camera app
        dispatchTakePictureIntent();
    }

    // Start the image capture on the phone
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                tempImageFile = createTempImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (tempImageFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.example.adhdread.fileprovider", tempImageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                imageCaptureLauncher.launch(takePictureIntent);
            }
        }
    }

    // Create a file to store the image
    private File createTempImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("temp_img", ".jpg", storageDir);
    }

    // Check the image current rotation and rotate if needed
    private Bitmap rotateImageIfRequired(Bitmap img) throws IOException {
        ExifInterface ei = new ExifInterface(tempImageFile.getAbsolutePath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    // Rotate the image bitmap
    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
    }

    // Use google Vision AI to get the text from the picture
    private String extractTextFromImage(Bitmap bitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.e(TAG, "TextRecognizer dependencies are not yet available.");
            return "";
        }

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> textSparseArray = textRecognizer.detect(frame);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < textSparseArray.size(); i++) {
            TextBlock textBlock = textSparseArray.valueAt(i);
            stringBuilder.append(textBlock.getValue());
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    // Format all of the words to the ADHD font (half of the words in bold)
    private String FormatWords(String text) {
        String[] splitByLines = text.split("\n");
        for (int i = 0; i < splitByLines.length; i++) {
            String[] splitBySpace = splitByLines[i].split(" ");
            for (int j = 0; j < splitBySpace.length; j++) {
                splitBySpace[j] = "<b>" + splitBySpace[j].substring(0, (int) Math.floor(splitBySpace[j].length()/2)) + "</b>" + splitBySpace[j].substring((int) Math.floor(splitBySpace[j].length()/2));
            }
            splitByLines[i] = String.join(" ", splitBySpace);
        }
        return String.join("<br>", splitByLines);
    }

    // On back press open up the camera
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dispatchTakePictureIntent();
    }
}
