package com.example.simplifiedvirtualtryon;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.FloatBuffer;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements Runnable {

    private int width = 192;
    private int height = 256;
    private ImageView personImageView;
    private ImageView clothingImageView;
    private ImageView resultImageView;
    private Module segmentation_model;
    private Module affine_model;
    private Module warping_model;
    private Module tryon_model;
    private ImageLoader imageLoader;
    private Bitmap person_image;
    Bitmap clothing_image;


    private Tensor people_inital_segment;
    private Tensor people_mask_tensor;

    private Tensor skeleton_tensor;
    private Tensor clothing_tensor;
    private Tensor clothing_mask_tensor;
    private Tensor people_tensor;
    private Tensor blurred_mask_tensor;
    private Button inference_button;
    private Bitmap blurred_mask;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private void loadPersonData(String personImageName) throws IOException {
        person_image = BitmapFactory.decodeStream(getAssets().open("people/" + personImageName));
        people_tensor = TensorImageUtils.bitmapToFloat32Tensor(person_image, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        people_mask_tensor = ProcessBitmap.standardiseMask(CustomTensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_mask/" + personImageName.replace(".jpg", ".png")))));
        people_inital_segment = CustomTensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_segment/" + personImageName.replace(".jpg", ".png"))));
        blurred_mask = ProcessBitmap.resizeMask(BitmapFactory.decodeStream(getAssets().open("people_mask/" + personImageName.replace(".jpg", ".png"))));
        blurred_mask_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(blurred_mask);
        skeleton_tensor = TensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_skeleton/" + personImageName)), TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        personImageView.setImageBitmap(person_image);
        }

    private void loadClothingData(String clothingName) throws IOException {
        clothing_image = BitmapFactory.decodeStream(getAssets().open("clothing/" + clothingName));
        clothing_tensor = TensorImageUtils.bitmapToFloat32Tensor(clothing_image, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        clothing_mask_tensor = ProcessBitmap.standardiseMask(CustomTensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("clothing_mask/" + clothingName))));
        clothingImageView.setImageBitmap(clothing_image);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int per_pos = intent.getIntExtra("person_index", -1);
        int cloth_pos = intent.getIntExtra("clothing_index", -1);
        if (per_pos != -1) {
            String personImageName = imageLoader.getPersonName(per_pos);
            try {
                loadPersonData(personImageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (cloth_pos != -1) {
            String clothingImageName = imageLoader.getClothingName(cloth_pos);
            try {
                loadClothingData(clothingImageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageLoader = ImageLoader.getInstance(this);
        String personImageName = imageLoader.getPersonName(-1);
        String clothingImageName = imageLoader.getClothingName(-1);
        personImageView = findViewById(R.id.PersonView);
        clothingImageView = findViewById(R.id.ClothingView);
        resultImageView = findViewById(R.id.ResultView);
        inference_button = findViewById(R.id.InferenceButton);
        try {
            loadPersonData(personImageName);
            loadClothingData(clothingImageName);
        } catch (IOException e) {
            Log.e("loading_image", "Error reading assets", e);
            finish();
        }

        personImageView.setImageBitmap(person_image);
        clothingImageView.setImageBitmap(clothing_image);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            segmentation_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "segmentation.ptl"));
            affine_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "affine.ptl"));
            warping_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "warpingg.ptl"));
            tryon_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "try_on_module.ptl"));
            Log.i("model_load", "Loaded all module!");
        } catch (IOException e) {
            Log.e("model_error", "Error reading assets", e);
            finish();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inference_button.setEnabled(true);
            }
        });
    }

    public void selectClothing(View view) {
        Intent myIntent = new Intent(MainActivity.this, ImageSelector.class);
        myIntent.putExtra("key", "clothing");
        MainActivity.this.startActivity(myIntent);
    }

    public void selectPerson(View view) {
        Intent myIntent = new Intent(MainActivity.this, ImageSelector.class);
        myIntent.putExtra("key", "person");
        MainActivity.this.startActivity(myIntent);
    }

    public void swapClothing(View view) {
        float[] original_right_arm_float = ProcessBitmap.getLabel(people_inital_segment, 11f);
        float[] original_left_arm_float = ProcessBitmap.getLabel(people_inital_segment, 13f);
        float[] original_torso_float = ProcessBitmap.getLabel(people_inital_segment, 4f);
        float[] original_face_float = ProcessBitmap.getLabel(people_inital_segment, 12f);

        clothing_tensor = ProcessBitmap.applyMask(clothing_tensor, clothing_mask_tensor, width, height);
        float[] clothing_float = clothing_tensor.getDataAsFloatArray();
        clothing_float = ProcessBitmap.calculateTanh(clothing_float);

        float[] people_mask_float = blurred_mask_tensor.getDataAsFloatArray();
        float[] skeleton_float = skeleton_tensor.getDataAsFloatArray();
        skeleton_float = ProcessBitmap.calculateTanh(skeleton_float);

        int totalLength = clothing_float.length + people_mask_float.length + skeleton_float.length;
        float[] phpmdData = new float[totalLength];
        System.arraycopy(people_mask_float, 0, phpmdData, 0, people_mask_float.length);
        System.arraycopy(clothing_float, 0, phpmdData, people_mask_float.length, clothing_float.length);
        System.arraycopy(skeleton_float, 0, phpmdData, clothing_float.length + people_mask_float.length, skeleton_float.length);
        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(7 * 256 * 192);
        for (float val : phpmdData)
            inTensorBuffer.put(val);
        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 7, 256, 192});
        Tensor outputTensor = segmentation_model.forward(IValue.from(inTensor)).toTensor();

        float[] scores = outputTensor.getDataAsFloatArray();
        float[][] generated_segments = ProcessBitmap.processSegmentationOutput(scores, width, height);
        float[] generated_torso_float = ProcessBitmap.standardiseMask(generated_segments[1]);
        float[] generated_right_arm_float = ProcessBitmap.standardiseMask(generated_segments[2]);
        float[] generated_left_arm_float = ProcessBitmap.standardiseMask(generated_segments[3]);

        float[] la = ProcessBitmap.arrayMultiply(original_left_arm_float, ProcessBitmap.inverseMask(generated_left_arm_float));
        float[] ra = ProcessBitmap.arrayMultiply(original_right_arm_float, ProcessBitmap.inverseMask(generated_right_arm_float));

        float[] resultant_la = ProcessBitmap.arrayMultiply(original_left_arm_float, ProcessBitmap.inverseMask(la));
        float[] resultant_ra = ProcessBitmap.arrayMultiply(original_right_arm_float, ProcessBitmap.inverseMask(ra));

        float[] final_la = ProcessBitmap.arrayMultiply(generated_left_arm_float, ProcessBitmap.inverseMask(resultant_la));
        float[] final_ra = ProcessBitmap.arrayMultiply(generated_right_arm_float, ProcessBitmap.inverseMask(resultant_ra));

        float[] generate_map = ProcessBitmap.fuseMap(generated_torso_float, final_ra, final_la);

        float[] inverse_torso_float = ProcessBitmap.inverseMask(original_torso_float);
        float[] inverse_generated_torso_float = ProcessBitmap.inverseMask(generated_torso_float);

        Tensor inverse_torso = CustomTensorImageUtils.bitmapToFloat32Tensor(ProcessBitmap.BitmapSpecificSegment(inverse_torso_float,  width, height));
        Tensor inverse_generated_torso = CustomTensorImageUtils.bitmapToFloat32Tensor(ProcessBitmap.BitmapSpecificSegment(inverse_generated_torso_float,  width, height));

        Tensor in_img_fore = ProcessBitmap.applyMask(people_tensor, people_mask_tensor, width, height);
        Tensor img_hole_hand = ProcessBitmap.applyMask(in_img_fore, inverse_torso, width, height);
        img_hole_hand = ProcessBitmap.applyMask(img_hole_hand, inverse_generated_torso, width, height);
        //img_hole_hand = ProcessBitmap.applyMask(img_hole_hand, occlude_tensor, width, height);
        float[] img_hole_hand_float = img_hole_hand.getDataAsFloatArray();
        img_hole_hand_float = ProcessBitmap.calculateTanh(img_hole_hand_float);
        Bitmap img_hole_hand_bmp = ProcessBitmap.RGBFloat2Bitmap(img_hole_hand_float, width, height);

        Bitmap generated_torso = ProcessBitmap.BitmapSpecificSegment(generated_torso_float, width, height);
        Bitmap generated_right_arm = ProcessBitmap.BitmapSpecificSegment(generated_right_arm_float, width, height);
        Bitmap generated_left_arm= ProcessBitmap.BitmapSpecificSegment(generated_left_arm_float, width, height);

        Tensor predicted_torso_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(generated_torso);
        Tensor predicted_right_arm_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(generated_right_arm);
        Tensor predicted_left_arm_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(generated_left_arm);

        float[] torso_float = predicted_torso_tensor.getDataAsFloatArray();

        totalLength = clothing_float.length + torso_float.length;
        float[] affineData = new float[totalLength];
        System.arraycopy(clothing_float, 0, affineData, 0, clothing_float.length);
        System.arraycopy(torso_float, 0, affineData, clothing_float.length, torso_float.length);
        inTensorBuffer = Tensor.allocateFloatBuffer(4 * 256 * 192);
        for (float val : affineData)
            inTensorBuffer.put(val);
        inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 4, 256, 192});
        outputTensor = affine_model.forward(IValue.from(inTensor)).toTensor();
        float[] affine_float = outputTensor.getDataAsFloatArray();
        affine_float = ProcessBitmap.calculateTanh(affine_float);

        totalLength = affine_float.length + torso_float.length + skeleton_float.length;
        float[] warpedData = new float[totalLength];
        System.arraycopy(affine_float, 0, warpedData, 0, affine_float.length);
        System.arraycopy(torso_float, 0, warpedData, affine_float.length, torso_float.length);
        System.arraycopy(skeleton_float, 0, warpedData, affine_float.length + torso_float.length, skeleton_float.length);
        inTensorBuffer = Tensor.allocateFloatBuffer(7 * 256 * 192);
        for (float val : warpedData)
            inTensorBuffer.put(val);
        inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 7, 256, 192});
        outputTensor = warping_model.forward(IValue.from(inTensor)).toTensor();
        outputTensor = ProcessBitmap.applyMask(outputTensor, predicted_torso_tensor, width, height);
        float[] warped_float = outputTensor.getDataAsFloatArray();
        warped_float = ProcessBitmap.calculateTanh(warped_float);

        Bitmap noise = ProcessBitmap.generateNoiseBitmap(width, height);
        float[] noise_float = CustomTensorImageUtils.bitmapToFloat32Tensor(noise).getDataAsFloatArray();

        float[] skin_mask = ProcessBitmap.arrayAdd(new float[][]{original_left_arm_float, original_right_arm_float, original_face_float});
        Tensor skin_mask_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(ProcessBitmap.BitmapSpecificSegment(skin_mask, width, height));
        Tensor skin_only_tensor = ProcessBitmap.applyMask(people_tensor, skin_mask_tensor, width, height);
        float[] skin_only_float = ProcessBitmap.calculateTanh(skin_only_tensor.getDataAsFloatArray());

        float[] skin_color_float = TensorImageUtils.bitmapToFloat32Tensor(ProcessBitmap.ger_average_color(skin_only_float, width, height),  TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB).getDataAsFloatArray();

        totalLength = img_hole_hand_float.length + generate_map.length + warped_float.length + skin_color_float.length + noise_float.length;
        float[] tryOnData = new float[totalLength];
        System.arraycopy(img_hole_hand_float, 0, tryOnData, 0, img_hole_hand_float.length);
        System.arraycopy(generate_map, 0, tryOnData, img_hole_hand_float.length, generate_map.length);
        System.arraycopy(warped_float, 0, tryOnData, img_hole_hand_float.length + generate_map.length, warped_float.length);
        System.arraycopy(skin_color_float, 0, tryOnData, img_hole_hand_float.length + generate_map.length + warped_float.length, skin_color_float.length);
        System.arraycopy(noise_float, 0, tryOnData, img_hole_hand_float.length + generate_map.length + warped_float.length + skin_color_float.length, noise_float.length);
        inTensorBuffer = Tensor.allocateFloatBuffer(11 * 256 * 192);
        for (float val : tryOnData)
            inTensorBuffer.put(val);
        inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 11, 256, 192});
        outputTensor = tryon_model.forward(IValue.from(inTensor)).toTensor();
        float[] finalImage = outputTensor.getDataAsFloatArray();
        finalImage = ProcessBitmap.calculateTanh(finalImage);
        Bitmap final_bmp = ProcessBitmap.RGBFloat2Bitmap(finalImage, width, height);
        resultImageView.setVisibility(View.VISIBLE);
        resultImageView.setImageBitmap(final_bmp);
        }

}