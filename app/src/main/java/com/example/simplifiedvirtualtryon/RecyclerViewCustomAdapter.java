package com.example.simplifiedvirtualtryon;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewCustomAdapter extends RecyclerView.Adapter<RecyclerViewCustomAdapter.ViewHolder> {
    private Bitmap[] bitmapsLocal;
    public RecyclerViewCustomAdapter(Bitmap[] bitmaps) {
        bitmapsLocal = bitmaps;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        RecyclerView recyclerView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.singleImage);
        }
        public ImageView getImageView(){
            return imageView;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_row_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.imageView.setImageBitmap(bitmapsLocal[position]);
    }

    @Override
    public int getItemCount() {
        return bitmapsLocal.length;
    }
}
