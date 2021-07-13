package com.example.recipeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    String title;
    String imageUrl;

    public Recipe(JSONObject jsonObject) throws JSONException {
        title = jsonObject.getString("title");
        imageUrl = jsonObject.getString("image");
    }

    public static List<Recipe> fromJsonArray(JSONArray movieJsonArray) throws JSONException {
        List<Recipe> recipes = new ArrayList<>();
        for (int i = 0; i < movieJsonArray.length(); i++) {
            recipes.add(new Recipe(movieJsonArray.getJSONObject(i)));
        }
        return recipes;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

}
