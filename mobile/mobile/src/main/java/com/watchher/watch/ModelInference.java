package com.watchher.watch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ModelInference {

   private final List<Tree> trees = new ArrayList<>();

   public ModelInference(InputStream jsonStream) throws Exception {
       String json = readStream(jsonStream);
       try {
           JSONArray forest = new JSONArray(json);
           for (int i = 0; i < forest.length(); i++) {
               trees.add(new Tree(forest.getJSONObject(i)));
           }
       } catch (JSONException e) {
           throw new IOException("Invalid model JSON", e);
       }
   }

   public double predict(double[] features) {
       double sum = 0.0;
       for (Tree t : trees) {
           sum += t.predict(features);
       }
       return sum / trees.size(); // probability
   }

   private static class Tree {
       int[] feature, left, right;
       double[] threshold;
       double[][] value;

       Tree(JSONObject obj) {
           try {
               feature = toIntArray(obj.getJSONArray("feature"));
               threshold = toDoubleArray(obj.getJSONArray("threshold"));
               left = toIntArray(obj.getJSONArray("left"));
               right = toIntArray(obj.getJSONArray("right"));
               value = to2DDoubleArray(obj.getJSONArray("value"));
           } catch (JSONException e) {
               throw new IllegalArgumentException("Invalid tree JSON", e);
           }
       }

       double predict(double[] x) {
           int node = 0;
           while (feature[node] != -2) { // -2 = leaf
               node = (x[feature[node]] <= threshold[node])
                       ? left[node]
                       : right[node];
           }
           return value[node][1];
       }
   }

   static int[] toIntArray(JSONArray arr) {
       int[] out = new int[arr.length()];
       for (int i = 0; i < arr.length(); i++) {
           out[i] = arr.optInt(i);
       }
       return out;
   }

   static double[] toDoubleArray(JSONArray arr) {
       double[] out = new double[arr.length()];
       for (int i = 0; i < arr.length(); i++) {
           out[i] = arr.optDouble(i);
       }
       return out;
   }

   static double[][] to2DDoubleArray(JSONArray arr) {
       double[][] out = new double[arr.length()][];
       for (int i = 0; i < arr.length(); i++) {
           JSONArray row = arr.optJSONArray(i);
           out[i] = row != null ? toDoubleArray(row) : new double[0];
       }
       return out;
   }

   private static String readStream(InputStream input) throws IOException {
       ByteArrayOutputStream buffer = new ByteArrayOutputStream();
       byte[] data = new byte[4096];
       int n;
       while ((n = input.read(data)) != -1) {
           buffer.write(data, 0, n);
       }
    return buffer.toString("UTF-8");
   }
}
