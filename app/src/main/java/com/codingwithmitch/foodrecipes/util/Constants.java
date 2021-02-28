package com.codingwithmitch.foodrecipes.util;

public class Constants {

    public static final int NETWORK_TIMEOUT = 3000;

    //API URL
    public static final String BASE_URL = "https://recipesapi.herokuapp.com";
    //API_KEY
    public static final String API_KEY = "";

    //constants for the network timeout
    public static final int CONNECTION_TIMEOUT = 10; // 10 seconds
    public static final int READ_TIMEOUT = 2; // 2 seconds
    public static final int WRITE_TIMEOUT = 2; // 2 seconds

    //refresh time for the data in cache
    public static final long RECIPE_REFRESH_TIME = 60 * 60 * 24 * 30;   // 30 days to refresh recipe



    // for the category session
    public static final String[] DEFAULT_SEARCH_CATEGORIES =
            {"Barbeque", "Breakfast", "Chicken", "Beef", "Brunch", "Dinner", "Wine", "Italian"};

    public static final String[] DEFAULT_SEARCH_CATEGORY_IMAGES =
            {
                    "barbeque",
                    "breakfast",
                    "chicken",
                    "beef",
                    "brunch",
                    "dinner",
                    "wine",
                    "italian"
            };


}
