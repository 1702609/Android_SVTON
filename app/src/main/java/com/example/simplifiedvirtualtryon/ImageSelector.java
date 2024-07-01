package com.example.simplifiedvirtualtryon;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageSelector extends AppCompatActivity{
    private RecyclerView recyclerView;
    private GridLayoutManager layoutManagerRecyclerView;
    private List<Bitmap> listOfImages_temp = new ArrayList<>();
    private Bitmap[] listOfImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_row_items);
        Intent intent = getIntent();
        String value = intent.getStringExtra("key");
        recyclerView = findViewById(R.id.recyclerView);
        layoutManagerRecyclerView = new GridLayoutManager(ImageSelector.this, 2);
        recyclerView.setLayoutManager(layoutManagerRecyclerView);
        ImageLoader imageLoader = ImageLoader.getInstance(this);
        List<String> image_names = null;
        if (value.matches("person")) {
            image_names = imageLoader.getPersonList();
            for (int i = 0; i < image_names.size(); i++) {
                Bitmap person_image;
                try {
                    person_image = BitmapFactory.decodeStream(getAssets().open("people/" + image_names.get(i)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                listOfImages_temp.add(person_image);
            }
        }
        else {
            image_names = imageLoader.getClothingList();
            for (int i = 0; i < image_names.size(); i++) {
                Bitmap clothing_image;
                try {
                    clothing_image = BitmapFactory.decodeStream(getAssets().open("clothing/" + image_names.get(i)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                listOfImages_temp.add(clothing_image);
            }
        }

        listOfImages = listOfImages_temp.toArray(new Bitmap[listOfImages_temp.size()]);

        OnImageClickListener clickListener = new OnImageClickListener() {
            @Override
            public void onImageClick(int position) {
                Intent intent = new Intent(ImageSelector.this, MainActivity.class);
                if (value.matches("person")) {
                    intent.putExtra("person_index", position);
                    }
                else {
                    intent.putExtra("clothing_index", position);
                    }
                startActivity(intent);
            }
        };

        RecyclerViewCustomAdapter recyclerViewCustomAdapter;
        recyclerViewCustomAdapter = new RecyclerViewCustomAdapter(listOfImages, clickListener);
        recyclerView.setAdapter(recyclerViewCustomAdapter);
    }

}
