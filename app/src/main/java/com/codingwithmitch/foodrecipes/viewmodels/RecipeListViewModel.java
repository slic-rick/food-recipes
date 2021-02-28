package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repositories.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    // for displaying the CATEGORIES or RECIPES screen
    public enum ViewState {CATEGORIES, RECIPES}
    private static final String TAG = "RecipeListViewModel";
    public static final String QUERY_EXHAUSTED = "Query is exhausted.";

    // live data that get viewState
    private MutableLiveData<ViewState> viewState;               // live data on an enum
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();
    private RecipeRepository recipeRepository;

    // query extras
    private boolean isQueryExhausted;
    private String query;
    private int pageNumber;
    private boolean isPerformingQuery;
    private boolean cancelRequest;
    private long requestStartTime;



    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = RecipeRepository.getInstance(application);
        init();

    }

    private void init(){
        if(viewState == null){
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.CATEGORIES);
        }
    }
    public LiveData<ViewState> getViewstate(){
        return viewState;
    }

    public LiveData<Resource<List<Recipe>>> getRecipes(){
        return recipes;
    }



    public void searchRecipesApi(String query, int pageNumber){
        if(!isPerformingQuery){
            if(pageNumber == 0){
                pageNumber = 1;
            }
            //saving the query
            this.pageNumber = pageNumber;
            this.query = query;
            isQueryExhausted = false;
            executeSearch();
        }
    }

    public void setViewCategories(){
        viewState.setValue(ViewState.CATEGORIES);
    }

    private void executeSearch(){

        requestStartTime = System.currentTimeMillis();
        isPerformingQuery = true;

        //set view state to recipes since we are displaying recipes
        viewState.setValue(ViewState.RECIPES);

        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository.searchRecipesApi(query, pageNumber);
        recipes.addSource(repositorySource, new Observer<Resource<List<Recipe>>>() {
            @Override
            public void onChanged(@Nullable Resource<List<Recipe>> listResource) {
                if(!cancelRequest){                         // if cancelled while searching

                    if(listResource != null){

                        recipes.setValue(listResource);                     // sending data to mutable livedata
                        if(listResource.status == Resource.Status.SUCCESS ){
                            Log.d(TAG, "onChanged: REQUEST TIME: " + (System.currentTimeMillis() - requestStartTime) / 1000 + " seconds.");
                            isPerformingQuery = false;                                                  // we got the
                            if(listResource.data != null) {

                                if (listResource.data.size() == 0) {

                                    // the query is exhausted
                                    Log.d(TAG, "onChanged: query is EXHAUSTED...");
                                    recipes.setValue(new Resource<List<Recipe>>(
                                            Resource.Status.ERROR,
                                            listResource.data,
                                            QUERY_EXHAUSTED
                                    ));
                                    isPerformingQuery = true;
                                }
                            }
                            // must remove or it will keep listening to repository
                            recipes.removeSource(repositorySource);
                        }
                        else if(listResource.status == Resource.Status.ERROR ){
                            isPerformingQuery = false;
                            recipes.removeSource(repositorySource);
                        }
                    }
                    else{
                        recipes.removeSource(repositorySource);
                    }

                } else{
                    recipes.removeSource(repositorySource);
                }

            }
        });

    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void searchNextPage() {
        //getting the next set of recipes, page++
        if (!isQueryExhausted && !isPerformingQuery) {
            pageNumber++;
            executeSearch();
        }
    }

    public void cancelSearchRequest(){
        if(isPerformingQuery){
            Log.d(TAG, "cancelSearchRequest: canceling the search request.");
            cancelRequest = true;
            isPerformingQuery = false;
            pageNumber = 1;
        }
    }
}















