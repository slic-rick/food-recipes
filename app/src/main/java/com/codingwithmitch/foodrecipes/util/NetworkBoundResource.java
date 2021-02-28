package com.codingwithmitch.foodrecipes.util;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

// CacheObject: Type for the Resource data.     (database cache)
// RequestObject: Type for the API response.    (network request)
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    private static final String TAG = "NetworkBoundResource";

    private AppExecutors appExecutors;

    //livedata for getting data from cache db
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        init();
    }

    private void init(){

        // update LiveData for loading status
        //initially display loading status b4 getting data from cache
        results.setValue((Resource<CacheObject>) Resource.loading(null));

        // observe LiveData source from local db
        final LiveData<CacheObject> dbSource = loadFromDb();

        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {

                // remove the source that we are observing
                results.removeSource(dbSource);

                if(shouldFetch(cacheObject)){

                    // get data from the network
                    fetchFromNetwork(dbSource);

                }
                else{
                    //get data from cache db
                    results.addSource(dbSource, new Observer<CacheObject>() {
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.success(cacheObject));

                        }
                    });
                }
            }
        });
    }

    /**
     * 1) observe local db
     * 2) if <condition/> query the network
     * 3) stop observing the local db
     * 4) insert new data into local db
     * 5) begin observing local db again to see the refreshed data from network
     * @param dbSource
     */


    private void fetchFromNetwork(final LiveData<CacheObject> dbSource){

        Log.d(TAG, "fetchFromNetwork: called.");

        // update LiveData for loading status
        results.addSource(dbSource, new Observer<CacheObject>() {
            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                setValue(Resource.loading(cacheObject));
            }
        });

        final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();

        results.addSource(apiResponse, new Observer<ApiResponse<RequestObject>>() {
            @Override
            public void onChanged(@Nullable final ApiResponse<RequestObject> requestObjectApiResponse) {

                results.removeSource(dbSource);
                results.removeSource(apiResponse);

                /*
                    3 cases:
                       1) ApiSuccessResponse
                       2) ApiErrorResponse
                       3) ApiEmptyResponse
                 */

                if(requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse){
                    Log.d(TAG, "onChanged: ApiSuccessResponse.");

                    appExecutors.diskIO().execute(new Runnable() {
                        @Override
                        public void run() {

                            // save the response to the local db
                            saveCallResult((RequestObject) processResponse((ApiResponse.ApiSuccessResponse)requestObjectApiResponse));

                            //reading data from cache?
                            appExecutors.mainThread().execute(new Runnable() {
                                @Override
                                public void run() {
                                    results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                        @Override
                                        public void onChanged(@Nullable CacheObject cacheObject) {
                                            setValue(Resource.success(cacheObject));
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
                else if(requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse){
                    Log.d(TAG, "onChanged: ApiEmptyResponse");

                    //reading data from cache
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            results.addSource(loadFromDb(), new Observer<CacheObject>() {
                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });
                }
                else if(requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse){
                    Log.d(TAG, "onChanged: ApiErrorResponse.");

                    results.addSource(dbSource, new Observer<CacheObject>() {

                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(
                                    Resource.error(
                                            ((ApiResponse.ApiErrorResponse) requestObjectApiResponse).getErrorMessage(),
                                            cacheObject
                                    )
                            );
                        }
                    });
                }
            }
        });
    }

    private CacheObject processResponse(ApiResponse.ApiSuccessResponse response){
        //returns the body of the response?
        return (CacheObject) response.getBody();

    }

    private void setValue(Resource<CacheObject> newValue){

        if(results.getValue() != newValue){
            //replace value in the cache  with @newValue, if value in cache changed
            results.setValue(newValue);
        }

    }

    // Called to save the result of the API response into the database.
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    // Called with the data in the database to decide whether to fetch
    // potentially updated data from the network.
    //checks if we should load data from cache db or not
    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    // Called to get the cached data from the database.
    @NonNull @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    // Called to create the API call.
    @NonNull @MainThread
    protected abstract LiveData<ApiResponse<RequestObject>> createCall();

    // Returns a LiveData object that represents the resource that's implemented
    // in the base class.
    public final LiveData<Resource<CacheObject>> getAsLiveData(){
        return results;
    };


}
