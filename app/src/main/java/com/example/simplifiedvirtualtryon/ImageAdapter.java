package com.example.simplifiedvirtualtryon;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ImageAdapter extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManagerRecyclerView;
    private RecyclerViewCustomAdapter recyclerViewCustomAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_row_items);


        recyclerView = findViewById(R.id.recyclerView);
        // Liner Layout
        layoutManagerRecyclerView = new LinearLayoutManager(ImageAdapter.this);
        // GRID Layout
        //layoutManagerRecyclerView = new GridLayoutManager(MainActivity.this, 2);

        recyclerView.setLayoutManager(layoutManagerRecyclerView);
    }

//    public void buttonRecyclerViewUpdate(View view){
//        StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
//        StorageVolume storageVolume = storageManager.getStorageVolumes().get(0); // internal memory/ storage
//
//        File fileImage = new File(storageVolume.getDirectory().getPath() + "/Download/images.jpeg");
//        File fileImage1 = new File(storageVolume.getDirectory().getPath() + "/Download/images1.jpeg");
//
//        Bitmap bitmap = BitmapFactory.decodeFile(fileImage.getPath());
//        Bitmap bitmap1 = BitmapFactory.decodeFile(fileImage1.getPath());
//
//        Bitmap[] bitmaps = {bitmap, bitmap1};
//        recyclerViewCustomAdapter = new RecyclerViewCustomAdapter(bitmaps);
//        recyclerView.setAdapter(recyclerViewCustomAdapter);
//    }

}
