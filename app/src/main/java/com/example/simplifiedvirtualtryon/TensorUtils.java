package com.example.simplifiedvirtualtryon;

import org.pytorch.Tensor;
import java.util.ArrayList;
import java.util.List;

public class TensorUtils{
    public static Tensor greaterThan(Tensor tensor, float value) {
        float[] data = tensor.getDataAsFloatArray();
        float[] resultData = new float[data.length];

        for (int i = 0; i < data.length; i++) {
            resultData[i] = data[i] > value ? 1f : 0f;
        }

        return Tensor.fromBlob(resultData, tensor.shape());
    }

    public static Tensor concatenate(Tensor[] tensors, int dim) {
        if (tensors.length == 0) {
            throw new IllegalArgumentException("Tensors array must have at least one tensor.");
        }

        // Assuming tensors have the same shape except for the concatenation dimension
        long[] shape = tensors[0].shape();
        int numTensors = tensors.length;
        int concatDimSize = 0;

        for (Tensor tensor : tensors) {
            concatDimSize += tensor.shape()[dim];
        }

        long[] newShape = shape.clone();
        newShape[dim] = concatDimSize;

        float[] concatenatedData = new float[(int) (Tensor.numel(newShape))];

        int offset = 0;
        for (Tensor tensor : tensors) {
            float[] data = tensor.getDataAsFloatArray();
            System.arraycopy(data, 0, concatenatedData, offset, data.length);
            offset += data.length;
        }

        return Tensor.fromBlob(concatenatedData, newShape);
    }
}