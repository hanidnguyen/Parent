package ca.cmpt276.ParentApp;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/*
    Add Child Activity
    -   Takes in the tapped name in configuration activity, and display in edit text.
    -   User can edit text and save name or delete name
    -   When add an empty name, snackbar appear to inform user action can not be done.

    References:
    -   https://stackoverflow.com/questions/4715044/android-how-to-convert-whole-imageview-to-bitmap
    -   https://stackoverflow.com/questions/9224056/android-bitmap-to-base64-string
    -   https://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
    -   https://www.youtube.com/watch?v=foOp5Dq1Ypk
    -   https://www.youtube.com/watch?v=7v9afVcQCxw
    -   https://www.youtube.com/watch?v=2tRw6Q2JXGo

 */

public class Add_Child_Activity extends AppCompatActivity {

    private ImageView profilePicture;
    private String image_path;
    private String name;
    private AlertDialog alertDialogProfilePicture;
    private ActivityResultLauncher<Intent> camera_launcher;
    private TextInputEditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        setup_params();

        setup_back_button();

        setup_camera_launcher();
        setup_submit_button();
        setup_camera_button();
    }

    private void setup_params() {
        profilePicture = findViewById(R.id.profile_picture);
        image_path = "";
        input = findViewById(R.id.add_name_edit_text);
    }

    private void setup_back_button() {
        ImageView back_button = findViewById(R.id.add_name_back_button);
        back_button.setOnClickListener(view -> Add_Child_Activity.super.onBackPressed());
    }

    private void setup_camera_launcher() {
        camera_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null){
                            Bundle extras = data.getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            name = String.valueOf(input.getText());
                            image_path = saveToInternalStorage(imageBitmap,name);
                            loadImageFromStorage(image_path,name,profilePicture);
                        }
                    }
                }
        );

    }

    //Submit profile back to config, send back image and name of child
    private void setup_submit_button() {
        Button button = findViewById(R.id.add_name_button);
        button.setOnClickListener(view -> {
            input = findViewById(R.id.add_name_edit_text);
            if(String.valueOf(input.getText()).equals("")){
                Snackbar.make(view,"Cannot add empty name",Snackbar.LENGTH_LONG).show();
            } else {
                String original = String.valueOf(input.getText());
                String capitalizeFirstLetter = original.substring(0, 1).toUpperCase() + original.substring(1);

                Intent intent = new Intent();
                intent.putExtra("NAME", capitalizeFirstLetter);

                profilePicture.buildDrawingCache();
                Bitmap bitmap = profilePicture.getDrawingCache();
                saveToInternalStorage(bitmap,capitalizeFirstLetter);
                intent.putExtra("PICTURE", image_path);

                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }

    private void setup_camera_button() {
        profilePicture.setOnClickListener(view -> chooseProfilePicture());
    }

    private void chooseProfilePicture() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Add_Child_Activity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_dialog_profile_picture, null);

        builder.setCancelable(false);
        builder.setView(dialogView);
        builder.setTitle(R.string.add_image);

        ImageView camera_icon = dialogView.findViewById(R.id.imageView_CameraIcon);
        ImageView gallery_icon = dialogView.findViewById(R.id.imageView_GalleryIcon);

        alertDialogProfilePicture = builder.create();
        alertDialogProfilePicture.show();

        camera_icon.setOnClickListener(view -> {
            takePictureFromCamera();
            alertDialogProfilePicture.cancel();
            TextView text = findViewById(R.id.touch_add_picture);
            text.setVisibility(View.INVISIBLE);
        });

        gallery_icon.setOnClickListener(view -> {
            takePictureFromGallery();
            alertDialogProfilePicture.cancel();
            TextView text = findViewById(R.id.touch_add_picture);
            text.setVisibility(View.INVISIBLE);
        });
    }

    private void takePictureFromGallery() {
        // start get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    private void takePictureFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera_launcher.launch(intent);
    }


    //pass in bitmap of image and filename (which is child's name) to save image
    private String saveToInternalStorage(Bitmap bitmapImage,String filename){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("children_images", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,filename + ".jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    //pass in filename (which is child's name) and the image path to load image.
    private void loadImageFromStorage(String path, String filename, ImageView imageView)
    {

        try {
            File f=new File(path, filename + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            imageView.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                profilePicture.setImageURI(resultUri);
                Bitmap imageBitmap = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        imageBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), resultUri));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                assert imageBitmap!=null;
                image_path = saveToInternalStorage(imageBitmap,String.valueOf(input.getText()));
            }
        }
    }

}