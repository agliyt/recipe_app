package com.example.recipeapp.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.example.recipeapp.BuildConfig;
import com.example.recipeapp.R;
import com.example.recipeapp.models.ParseRecipe;
import com.example.recipeapp.models.Recipe;
import com.example.recipeapp.adapters.RecipesAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecipesFragment extends Fragment implements RecipesAdapter.OnClickListener {

    public static final String REST_CONSUMER_KEY = BuildConfig.CONSUMER_KEY;
    public static final String BASE_URL = "https://api.spoonacular.com/recipes/findByIngredients";
    public static final String TAG = "RecipesFragment";
    private String SEARCH_RECIPES_URL;

    private RecyclerView rvRecipes;
    private List<Recipe> allRecipes;
    private List<Recipe> userRecipes;
    private RecipesAdapter adapter;
    private ParseUser currentUser;
    private List<String> ingredients;
    private String ingredientsString;

    public RecipesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recipes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvRecipes = view.findViewById(R.id.rvRecipes);

        allRecipes = new ArrayList<>();
        userRecipes = new ArrayList<>();
        adapter = new RecipesAdapter(getContext(), allRecipes, RecipesFragment.this);

        // create data for one row in list
        // create adapter
        // create data source
        // set adapter on recycler view
        rvRecipes.setAdapter(adapter);
        // set layout manager on recycler view
        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));

        currentUser = ParseUser.getCurrentUser();
        ingredients = (List<String>) currentUser.get("ingredientsOwned");
        queryUserRecipes();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ingredients.size()-1; i++) {
            sb.append(ingredients.get(i));
            sb.append(",+");
        }
        sb.append(ingredients.get(ingredients.size()-1));
        ingredientsString = sb.toString();

        SEARCH_RECIPES_URL = BASE_URL + "?ingredients=" + ingredientsString + "&number=20&ranking=2&apiKey=" + REST_CONSUMER_KEY;
        Log.i(TAG, SEARCH_RECIPES_URL);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(SEARCH_RECIPES_URL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONArray jsonArray = json.jsonArray;
                try {
                    allRecipes.addAll(Recipe.fromJsonArray(jsonArray));

                    // add all user recipes that fit into allRecipes (based on # ingredients missed)
                    for (int i = 0; i < userRecipes.size(); i++) {
                        for (Recipe currAllRecipe : allRecipes) {
                            Recipe currUserRecipe = userRecipes.get(i);
                            Log.i(TAG, currAllRecipe.getTitle());
                            if (currUserRecipe.getMissedIngredientsCount() <= currAllRecipe.getMissedIngredientsCount()) {
                                allRecipes.add(allRecipes.indexOf(currAllRecipe), currUserRecipe);
                                break;
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    Log.i(TAG, "Recipes: " + allRecipes.size());
                } catch (JSONException e) {
                    Log.e(TAG, "Hit json exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.d(TAG, "onFailure: " + response + throwable);
            }
        });
    }

    protected void queryUserRecipes() {
        ParseQuery<ParseRecipe> query = ParseQuery.getQuery(ParseRecipe.class);
        query.findInBackground(new FindCallback<ParseRecipe>() {
            @Override
            public void done(List<ParseRecipe> parseRecipes, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting recipes", e);
                    return;
                }
                try {
                    userRecipes.clear();
                    userRecipes.addAll(Recipe.fromParseRecipeArray(parseRecipes));
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onItemClicked(int position) {
        final Recipe recipe = allRecipes.get(position);
        FragmentTransaction ft =  getActivity().getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        RecipeDetailsFragment recipeDetailsFragment = new RecipeDetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable("recipe", recipe);
        recipeDetailsFragment.setArguments(bundle);
        ft.replace(R.id.flRecipesContainer, recipeDetailsFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onFavoritesClicked(int position) {
        final Recipe recipe = allRecipes.get(position);
        List<String> favoriteApiRecipes = (List<String>) currentUser.get("recipesFavoritedAPI");
        List<String> favoriteUserRecipes = (List<String>) currentUser.get("recipesFavoritedUser");
        // handle whether recipe is in favorites or not
        if (recipe.isFromApi()) {
            if (favoriteApiRecipes.contains(String.valueOf(recipe.getId()))) { // already favorited
                favoriteApiRecipes.remove(String.valueOf(recipe.getId()));
            } else { // not favorited yet
                favoriteApiRecipes.add(String.valueOf(recipe.getId()));
            }
            // Other attributes than "recipesFavoritedAPI" will remain unchanged!
            currentUser.put("recipesFavoritedAPI", favoriteApiRecipes);

            // Saves the object.
            currentUser.saveInBackground(e -> {
                if(e==null){
                    //Save successfull
                    Log.i(TAG, "Save successful: " + favoriteApiRecipes.toString());
                }else{
                    // Something went wrong while saving
                    Log.e(TAG, "Save unsuccessful", e);
                }
            });
        } else {
            if (favoriteUserRecipes.contains(recipe.getObjectId())) { // already favorited
                favoriteUserRecipes.remove(recipe.getObjectId());
            } else { // not favorited yet
                favoriteUserRecipes.add(recipe.getObjectId());
            }
            // Other attributes than "recipesFavoritedUser" will remain unchanged!
            currentUser.put("recipesFavoritedUser", favoriteUserRecipes);

            // Saves the object.
            currentUser.saveInBackground(e -> {
                if(e==null){
                    //Save successful
                    Log.i(TAG, "Save successful: " + favoriteUserRecipes.toString());
                }else{
                    // Something went wrong while saving
                    Log.e(TAG, "Save unsuccessful", e);
                }
            });
        }
        adapter.notifyDataSetChanged();
    }
}