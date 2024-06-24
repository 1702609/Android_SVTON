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
    public static class ViewHolder extends  RecyclerView.ViewHolder{
        private final ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
        public ImageView getImageView(){
            return imageView;
        }
    }

    @NonNull
    @Override
    public RecyclerViewCustomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_row_items, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull RecyclerViewCustomAdapter.ViewHolder holder, int position) {
        holder.getImageView().setImageBitmap(bitmapsLocal[position]);
    }

    @Override
    public int getItemCount() {
        return bitmapsLocal.length;
    }
}
