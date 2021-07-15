package com.example.recipeapp.fragments;

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
import android.widget.Button;

import com.example.recipeapp.R;
import com.example.recipeapp.adapters.ComposeAdapter;
import com.example.recipeapp.adapters.RecipesAdapter;
import com.example.recipeapp.models.Recipe;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.

 */
public class MakeRecipeFragment extends Fragment implements ComposeAdapter.OnClickListener {

    public static final String TAG = "MakeRecipesFragment";

    private RecyclerView rvUserRecipes;
    private List<Recipe> allRecipes;
    private ComposeAdapter adapter;
    private Button btnCompose;

    public MakeRecipeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_make_recipe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvUserRecipes = view.findViewById(R.id.rvUserRecipes);

        allRecipes = new ArrayList<>();
        adapter = new ComposeAdapter(getContext(), allRecipes, MakeRecipeFragment.this);

        // create data for one row in list
        // create adapter
        // create data source
        // set adapter on recycler view
        rvUserRecipes.setAdapter(adapter);
        // set layout manager on recycler view
        rvUserRecipes.setLayoutManager(new LinearLayoutManager(getContext()));

        queryUserRecipes();

        btnCompose = view.findViewById(R.id.btnCompose);

        btnCompose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "compose button clicked");
                ComposeFragment composeFragment = new ComposeFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(((ViewGroup)getView().getParent()).getId(), composeFragment, "findThisFragment")
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    protected void queryUserRecipes() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Recipe");
        query.include("author");
        query.setLimit(20);
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> recipes, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with getting posts", e);
                    return;
                }
                recipes.clear();
                try {
                    allRecipes = Recipe.fromArrayList(recipes);
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onItemClicked(int position) {

    }
}