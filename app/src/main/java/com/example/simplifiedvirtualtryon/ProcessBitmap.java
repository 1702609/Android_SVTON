package com.example.simplifiedvirtualtryon;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.pytorch.Tensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProcessBitmap {
    public static Bitmap RGBFloat2Bitmap(float[] data, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width * height; i++) {
            int r = (int) ((data[i] * 0.5 + 0.5) * 255f);
            int g = (int) ((data[i + (width * height)] * 0.5 + 0.5) * 255f);
            int b = (int) ((data[i + (width * height) * 2] * 0.5 + 0.5) * 255f);
            int x = i / width;
            int y = i % width;
            int color = Color.rgb(r, g, b);
            bmp.setPixel(y, x, color);
        }
        return bmp;
    }

    // 0 --> Background: RGB: 0, 0, 0
    // 1 --> Torso: RGB: 50, 60, 35 --> 4
    // 2 --> Right_arm: RGB: 100, 75, 87 --> 11
    // 3 --> Left_arm: RGB: 150, 123, 134 --> 13

    public static Bitmap VisualiseWholeSegment(float[] data, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width * height; i++) {
            int r = 0;
            int g = 0;
            int b = 0;
            if (data[i] == 1f) {
                r = 50;
                g = 60;
                b = 35;
            } else if (data[i] == 2f) {
                r = 100;
                g = 75;
                b = 87;
            } else if (data[i] == 3f) {
                r = 250;
                g = 223;
                b = 234;
            }
            int x = i / width;
            int y = i % width;
            int color = Color.rgb(r, g, b);
            bmp.setPixel(y, x, color);
        }
        return bmp;
    }

    private static <T> T getMode(ArrayList<T> list) {
        HashMap<T, Integer> frequencyMap = new HashMap<>();

        for (T element : list) {
            frequencyMap.put(element, frequencyMap.getOrDefault(element, 0) + 1);
        }

        // Find the mode
        T mode = null;
        int maxCount = -1;
        for (Map.Entry<T, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }
        return mode;
    }

    public static Bitmap ger_average_color(float[] skin, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        ArrayList<Integer> mode_r = new ArrayList<>();
        ArrayList<Integer> mode_g = new ArrayList<>();
        ArrayList<Integer> mode_b = new ArrayList<>();

        int channel_length = width * height;
        for (int red = 0; red < channel_length; red++){
            int r = (int)((skin[red] * 0.5 + 0.5) * 255);
            if (r > 127) {
                mode_r.add(r);
                }
            }

            for (int green = 0; green < channel_length; green++){
                int g = (int)((skin[channel_length + green] * 0.5 + 0.5) * 255);
                if (g > 127) {
                    mode_g.add(g);
                }
            }

            for (int blue = 0; blue < channel_length; blue++){
                int b = (int)((skin[(channel_length*2) + blue] * 0.5 + 0.5) * 255);
                if (b > 127) {
                    mode_b.add(b);
                }
            }

            int avg_red = getMode(mode_r);
            int avg_green = getMode(mode_g);
            int avg_blue = getMode(mode_b);

            for (int i = 0; i< channel_length; i++) {
                int x = i / width;
                int y = i % width;
                int color = Color.rgb(avg_red, avg_green, avg_blue);
                bmp.setPixel(y, x, color);
            }
            return bmp;
        }
    public static Bitmap BitmapSpecificSegment(float[] data, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < width * height; i++) {
            int r = 0;
            int g = 0;
            int b = 0;
            if (data[i] == 1f) {
                r = 255;
                g = 255;
                b = 255;
            }

            int x = i / width;
            int y = i % width;
            int color = Color.rgb(r, g, b);
            bmp.setPixel(y, x, color);
        }
        return bmp;
    }

    public static Bitmap resizeMask(Bitmap mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        Bitmap small = Bitmap.createScaledBitmap(mask, width/8, height/8, true);
        Bitmap big = Bitmap.createScaledBitmap(small, width, height, true);
        return big;
    }

    public static float[] applySigmoid(float[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (1 / (1 + Math.exp(-data[i])));
        }
        return data;
    }

    public static float[][] processSegmentationOutput(float[] data, int width, int height) {
        float[] background;
        float[] torso;
        float[] right_arm;
        float[] left_arm;
        float[][] everything;
        int length = width * height;

        background = Arrays.copyOfRange(data, 0, length);
        torso = Arrays.copyOfRange(data, length, length*2);
        right_arm = Arrays.copyOfRange(data, length*2, length*3);
        left_arm = Arrays.copyOfRange(data, length*3, length*4);

        background = applySigmoid(background);
        torso = applySigmoid(torso);
        right_arm = applySigmoid(right_arm);
        left_arm = applySigmoid(left_arm);

        everything = new float[][]{background, torso, right_arm, left_arm};
        return everything;
    }

    public static float[] calculateTanh(float[] inputArray) {
        float[] resultArray = new float[inputArray.length];
        for (int i = 0; i < inputArray.length; i++) {
            double ex = Math.exp(inputArray[i]);
            double eNegX = Math.exp(-inputArray[i]);
            resultArray[i] = (float) ((ex - eNegX) / (ex + eNegX));
        }
        return resultArray;
    }

    public static Tensor standardiseMask(Tensor maskTensor){
        long[] shape = maskTensor.shape();
        float[] maskArray = maskTensor.getDataAsFloatArray();
        for (int i = 0; i < maskArray.length; i++) {
            if (maskArray[i] > 0.5){
                maskArray[i] = 1f;
            }
            else {
                maskArray[i] = 0f;
            }
        }
        return Tensor.fromBlob(maskArray, shape);
    }

    public static float[] standardiseMask(float[] mask){
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] > 0.5){
                mask[i] = 1f;
            }
            else {
                mask[i] = 0f;
            }
        }
        return mask;
    }

    public static Tensor applyMask(Tensor imageTensor, Tensor maskTensor, int width, int height) {
        float[] imageArray = imageTensor.getDataAsFloatArray();
        float[] maskArray = maskTensor.getDataAsFloatArray();
        long[] shape = imageTensor.shape();

        for (int i = 0; i < maskArray.length; i++) {
            if (maskArray[i] == 0f) {
                imageArray[i] = 0f;
                imageArray[i + width * height] = 0f;
                imageArray[i + width * height * 2] = 0f;
            }
        }
        return Tensor.fromBlob(imageArray, shape);
    }

    public static float[] processSegment(Tensor imageTensor) {
        float[] initial_segment_float = imageTensor.getDataAsFloatArray();
        for (int i=0; i<initial_segment_float.length; i++)
        {
            float element = initial_segment_float[i];
            int segmentIndex = Math.round(element * 255f);
            initial_segment_float[i] = (float) segmentIndex;
        }
        return initial_segment_float;
    }

    public static float[] getLabel(Tensor imageTensor, float wantedSegment) {
        float[] initial_segment_float = imageTensor.getDataAsFloatArray();
        for (int i=0; i<initial_segment_float.length; i++)
        {
            float element = initial_segment_float[i];
            int segmentIndex = Math.round(element * 255f);
            if (segmentIndex == wantedSegment)
                {
                initial_segment_float[i] = 1f;
                }
            else{
                initial_segment_float[i] = 0f;
            }
        }
        return initial_segment_float;
    }

    public static float[] inverseMask(float[] mask) {
        float[] copy = new float[mask.length];
        for (int i=0; i<copy.length; i++) {
            if (mask[i] == 1f) {
                copy[i] = 0f;
            }
            else {
                copy[i] = 1f;
            }
        }
    return copy;
    }

    public static float[] arrayMultiply(float[] a, float[] b) {
        float[] c = new float[a.length];
        for (int index = 0; index < a.length; index++) {
            c[index] = a[index] * b[index];
        }
        return c;
    }

    public static float[] fuseMap(float[] torso, float[] rightArm, float[] leftArm) {
        float[] resultant = new float[torso.length];
        for (int i = 0; i < resultant.length; i++) {
            resultant[i] = 0f;
            if (torso[i] == 1f) {
                resultant[i] = 1f;
            } else if (rightArm[i] == 1f) {
                resultant[i] = 2f;
            } else if (leftArm[i] == 1f) {
                resultant[i] = 3f;
            }
        }
        return resultant;
    }

    public static float[] arrayAdd(float[][] masks) {
        float[] resultant = new float[masks[0].length];
        for (int i = 0; i<masks.length; i++) {
            for (int j = 0; j<masks[0].length; j++) {
                if (masks[i][j] != 0){
                    resultant[j] = masks[i][j];
                }
            }
        }
        return resultant;
    }

    public static Bitmap generateNoiseBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Random random = new Random();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int noise = (int) (random.nextGaussian() * 128 + 128); // Gaussian noise

                int color = Color.rgb(noise, noise, noise); // Grayscale color
                bitmap.setPixel(x, y, color);
            }
        }
        return bitmap;
    }

}