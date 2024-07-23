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
import android.widget.Toast;

import org.pytorch.Device;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements Runnable {

    public static float[] mean_clothing = new float[] {0.73949153f, 0.70635068f, 0.71736564f};
    public static float[] std_clothing = new float[] {0.34867646f, 0.36374153f, 0.35065262f};

    public static float[] mean_candidate = new float[] {0.74112587f, 0.69617281f, 0.68865463f};
    public static float[] std_candidate = new float[] {0.2941623f, 0.30806473f, 0.30613222f};

    public static float[] mean_skeleton = new float[] {0.05440789f, 0.07170792f, 0.04121648f};
    public static float[] std_skeleton = new float[] {0.20046051f, 0.23692659f, 0.16482468f};

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
        people_tensor = TensorImageUtils.bitmapToFloat32Tensor(person_image, mean_candidate, std_candidate);
        people_mask_tensor = ProcessBitmap.standardiseMask(CustomTensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_mask/" + personImageName.replace(".jpg", ".png")))));
        people_inital_segment = CustomTensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_segment/" + personImageName.replace(".jpg", ".png"))));
        blurred_mask = ProcessBitmap.resizeMask(BitmapFactory.decodeStream(getAssets().open("people_mask/" + personImageName.replace(".jpg", ".png"))));
        blurred_mask_tensor = CustomTensorImageUtils.bitmapToFloat32Tensor(blurred_mask);
        skeleton_tensor = TensorImageUtils.bitmapToFloat32Tensor(BitmapFactory.decodeStream(getAssets().open("people_skeleton/" + personImageName)), mean_skeleton, std_skeleton);
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
            warping_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "gmm.ptl"));
            tryon_model = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "tom.ptl"));
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

    public Tensor concatenateFloat2Tensor(ArrayList<float[]> input_data, int channel){
        int totalLength = 0;
        for (int i = 0; i < input_data.size(); i++) {
            totalLength += input_data.get(i).length;
            }
        float[] combinedData = new float[totalLength];
        int cummulative_index = 0;

        for (int i1 = 0; i1 < input_data.size(); i1++) {
            float[] single_float = input_data.get(i1);
            System.arraycopy(single_float, 0, combinedData, cummulative_index, single_float.length);
            cummulative_index += single_float.length;
        }

        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(channel * 256 * 192);
        for (float val : combinedData){
            inTensorBuffer.put(val);}
        Tensor inTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, channel, 256, 192});
        return inTensor;
    }

    public void swapClothing(View view) {
        inference_button.setEnabled(false);
        long startTime = System.currentTimeMillis(); // or System.nanoTime()
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

        ArrayList<float[]> input_list = new ArrayList<>();
        input_list.add(people_mask_float);
        input_list.add(clothing_float);
        input_list.add(skeleton_float);
        Tensor inTensor = concatenateFloat2Tensor(input_list, 7);
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

        input_list = new ArrayList<>();
        input_list.add(clothing_float);
        input_list.add(torso_float);

        inTensor = concatenateFloat2Tensor(input_list, 4);
        outputTensor = affine_model.forward(IValue.from(inTensor)).toTensor();
        float[] affine_float = outputTensor.getDataAsFloatArray();
        affine_float = ProcessBitmap.calculateTanh(affine_float);

        input_list = new ArrayList<>();
        input_list.add(affine_float);
        input_list.add(torso_float);
        input_list.add(skeleton_float);

        inTensor = concatenateFloat2Tensor(input_list,7);
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
        float[] skin_color_float = TensorImageUtils.bitmapToFloat32Tensor(ProcessBitmap.ger_average_color(skin_only_float, width, height),  mean_clothing, std_clothing).getDataAsFloatArray();

        input_list = new ArrayList<>();
        input_list.add(img_hole_hand_float);
        input_list.add(generate_map);
        input_list.add(warped_float);
        input_list.add(skin_color_float);
        input_list.add(noise_float);

        inTensor = concatenateFloat2Tensor(input_list, 11);
        outputTensor = tryon_model.forward(IValue.from(inTensor)).toTensor();
        float[] finalImage = outputTensor.getDataAsFloatArray();
        finalImage = ProcessBitmap.calculateTanh(finalImage);
        Bitmap final_bmp = ProcessBitmap.RGBFloat2Bitmap(finalImage, width, height);
        resultImageView.setVisibility(View.VISIBLE);
        resultImageView.setImageBitmap(final_bmp);
        long endTime = System.currentTimeMillis(); // or System.nanoTime()
        long duration = endTime - startTime;
        Toast.makeText(this, "Execution time: " + duration + " milliseconds", Toast.LENGTH_LONG).show();
        inference_button.setEnabled(true);
    }
}