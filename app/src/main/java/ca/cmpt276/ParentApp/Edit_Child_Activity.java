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
import androidx.annotation.NonNull;
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

import ca.cmpt276.ParentApp.model.Child;

/*
    Edit Name Activity:
    -   Text input display tapped name
    -   Edit name, then press 'save' or 'delete' to save or delete name.
    -   Upon pressing back button, name will be reverted back to original.
 */

public class Edit_Child_Activity extends AppCompatActivity {

    private TextInputEditText input;
    private String image_path;
    private ImageView profilePicture;
    private AlertDialog alertDialogProfilePicture;
    private ActivityResultLauncher<Intent> camera_launcher;
    private ActivityResultLauncher<Intent> gallery_launcher;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_child);

        setup_params();

        setup_camera_launcher();
        setup_gallery_launcher();

        setup_back_button();
        setup_profile();
        setup_camera_button();
        setup_save_button();
        setup_delete_button();
    }

    private void setup_params() {
        profilePicture = findViewById(R.id.edit_profile_picture);
        image_path = getIntent().getStringExtra("IMAGE");
        input = findViewById(R.id.edit_name_edit_text);
    }

    private void setup_camera_launcher() {
        camera_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null){
                            Bundle extras = data.getExtras();
                            imageBitmap = (Bitmap) extras.get("data");
                            String name = String.valueOf(input.getText());
                            image_path = saveToInternalStorage(imageBitmap,name);
                            loadImageFromStorage(image_path,name,profilePicture);
                        }
                    }
                }
        );

    }

    private void setup_gallery_launcher(){
        gallery_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        // do your operation from here....
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            try {
                                imageBitmap = MediaStore.Images.Media.getBitmap(
                                        this.getContentResolver(),
                                        selectedImageUri);

                                profilePicture.setImageBitmap(imageBitmap);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
    }

    private void setup_back_button() {
        ImageView button = findViewById(R.id.edit_back_button);
        button.setOnClickListener(view -> Edit_Child_Activity.super.onBackPressed());
    }

    private void setup_profile() {
        Intent data = getIntent();
        Child current_child = data.getParcelableExtra("CHILD");

        input.setText(current_child.getName());

        profilePicture = findViewById(R.id.edit_profile_picture);
        String image = data.getStringExtra("IMAGE");
        loadImageFromStorage(image,current_child.getName(), profilePicture);
    }


    private void setup_camera_button() {
        profilePicture.setOnClickListener(view -> chooseProfilePicture());
    }

    private void setup_save_button(){
        Button submitBtn= findViewById(R.id.save_button);
        input = findViewById(R.id.edit_name_edit_text);
        submitBtn.setOnClickListener(view -> {
            if(String.valueOf(input.getText()).equals("")){
                Snackbar.make(view,"Cannot add empty name",Snackbar.LENGTH_LONG).show();
            } else {
                String original = String.valueOf(input.getText());
                String capitalizeFirstLetter = original.substring(0, 1).toUpperCase() + original.substring(1);

                Intent intent = new Intent();
                intent.putExtra("DELETE_BUTTON",false);
                intent.putExtra("NAME", capitalizeFirstLetter);

                profilePicture.buildDrawingCache();
                Bitmap bitmap = profilePicture.getDrawingCache();
                saveToInternalStorage(bitmap,capitalizeFirstLetter);
                intent.putExtra("PICTURE", image_path);

                intent.putExtra("INDEX", getIntent().getIntExtra("INDEX",-1));
                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }

    private void setup_delete_button() {
        ImageView deleteBtn = findViewById(R.id.delete_button);
        deleteBtn.setOnClickListener(view -> new AlertDialog.Builder(Edit_Child_Activity.this)
                .setTitle("Delete name")
                .setMessage("Are you sure to delete this entry?")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.putExtra("DELETE_BUTTON",true);
                    intent.putExtra("INDEX", getIntent().getIntExtra("INDEX",-1));
                    setResult(RESULT_OK,intent);
                    finish();
                })
                .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.cancel())
                .setIcon(R.drawable.delete_icon)
                .show());
    }


    private void chooseProfilePicture() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Edit_Child_Activity.this);
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
            TextView text = findViewById(R.id.touch_edit_picture);
            text.setVisibility(View.INVISIBLE);
        });

        gallery_icon.setOnClickListener(view -> {
            takePictureFromGallery();
            alertDialogProfilePicture.cancel();
            TextView text = findViewById(R.id.touch_edit_picture);
            text.setVisibility(View.INVISIBLE);
        });
    }

    private void takePictureFromGallery() {
    //DEPRECATED SINCE OCT 07, 2023
//        // start get image for cropping and then use the image in cropping activity
//        CropImage.activity()
//                .setGuidelines(CropImageView.Guidelines.ON)
//                .start(this);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        gallery_launcher.launch(intent);
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
        //DEPRECATED SINCE OCT 07, 2023
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            if (resultCode == RESULT_OK) {
//                Uri resultUri = result.getUri();
//                profilePicture.setImageURI(resultUri);
//                Bitmap imageBitmap = null;
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                    try {
//                        imageBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), resultUri));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    try {
//                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                assert imageBitmap!=null;
//                image_path = saveToInternalStorage(imageBitmap,String.valueOf(input.getText()));
//            }
//        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("EDIT_TEXT",String.valueOf(input.getText()));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        input.setText(savedInstanceState.getString("EDIT_TEXT"));
    }
}