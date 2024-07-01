package com.example.simplifiedvirtualtryon;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewCustomAdapter extends RecyclerView.Adapter<RecyclerViewCustomAdapter.ViewHolder> {
    private Bitmap[] bitmapsLocal;
    private OnImageClickListener onImageClickListener;

    public RecyclerViewCustomAdapter(Bitmap[] bitmaps, OnImageClickListener listener) {
        bitmapsLocal = bitmaps;
        this.onImageClickListener = listener; // Initialize with listener passed from outside

    }


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        OnImageClickListener onImageClickListenerInside;
        ImageView imageView;
        public ViewHolder(@NonNull View itemView, OnImageClickListener onImageClickListener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.singleImage);
            this.onImageClickListenerInside = onImageClickListener;
            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            onImageClickListenerInside.onImageClick(position);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_row_items, parent, false);
        return new ViewHolder(view, onImageClickListener);
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
