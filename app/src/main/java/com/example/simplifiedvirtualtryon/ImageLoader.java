package com.example.simplifiedvirtualtryon;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImageLoader{
    private static ImageLoader INSTANCE;
    private Random random = new Random();

    private List<String> listOfPersonImages = new ArrayList<>();
    private List<String> listOfClothingImages = new ArrayList<>();
    private ImageLoader(Context context) {
        populateImageArray(context);
    }

    public static ImageLoader getInstance(Context c) {
        if(INSTANCE == null) {
            INSTANCE = new ImageLoader(c);
        }
        return INSTANCE;
    }

    public void populateImageArray(Context context){
        AssetManager assetManager = context.getAssets();

        try {
            // List all files in the assets directory
            String[] personFiles = assetManager.list("people");
            String[] clothingFiles = assetManager.list("clothing");

            // Iterate through the files
            for (String file : personFiles) {
                // Check if file is an image file (you can add more formats if needed)
                if (file.endsWith(".jpg")) {
                    // Add image file name to the list
                    listOfPersonImages.add(file);
                }
            }
            for (String file : clothingFiles) {
                // Check if file is an image file (you can add more formats if needed)
                if (file.endsWith(".jpg")) {
                    // Add image file name to the list
                    listOfClothingImages.add(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClothingName(){
        int clothing_id = random.nextInt(listOfClothingImages.size());
        String clothingImageName = listOfClothingImages.get(clothing_id);
        return clothingImageName;
    }

    public String getPersonName(){
        int person_id = random.nextInt(listOfPersonImages.size());
        String personImageName = listOfPersonImages.get(person_id);
        return personImageName;
    }

}
