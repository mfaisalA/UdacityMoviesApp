package com.example.faisal.udacitymoviesapp;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Faisal on 10/30/2015.
 */
public class MoviesGridFragment extends Fragment {
    private GridView moviesGridView;
    private List<String> moviesList;
    private MoviesGridAdapter mGridAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        getMovieList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.movies_grid_layout, container, false);

//        String[] sList = {"First", "Second", "Third", "Fourth"};
//        ArrayList<String> list = new ArrayList<>(Arrays.asList(sList));
        moviesList  = new ArrayList<>();
        mGridAdapter = new MoviesGridAdapter(getActivity(), moviesList);
        moviesGridView = (GridView) rootView.findViewById(R.id.movie_grid);
        moviesGridView.setAdapter(mGridAdapter);
        return rootView;
    }

    public class FetchMoviesList extends AsyncTask<String, Void, String[]> {
        private final String LOG_TAG = FetchMoviesList.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            // if there is no zip code, there is nothing to lookup, check the size of params
            if (params == null) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieListJsonStr = null;

            try {
                // Construct the URL for the movieDB query
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key=[YOUR API KEY]");
                //String sortBy = "poularity.desc";
                final String API_KEY = "912fd914d5ff9c7ae34051ca4bfbcd3d";

                // Construct the uri for open weather Map Query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_BY_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";


                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_BY_PARAM, params[0])
                        .appendQueryParameter(API_KEY_PARAM, API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.i(LOG_TAG, "Built URI, " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
               movieListJsonStr = buffer.toString();
                Log.i(LOG_TAG, movieListJsonStr);
                try {
                    return getMovieDataFromJson(movieListJsonStr);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    e.printStackTrace();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
            moviesList.clear();
            if ( moviesList.addAll(Arrays.asList(strings))){
                mGridAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getMovieDataFromJson(String movieListJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String MDB_RESULTS = "results";
        final String MDB_ORIGINAL_TITLE = "original_title";
        final String MDB_MOVIE_POSTER = "poster_path";
        final String MDB_PLOT_SYNOPSIS = "overview";
        final String MDB_RATING = "vote_average";
        final String MDB_RELEASE_DATE = "release_date";

        JSONObject movieListJson = new JSONObject(movieListJsonStr);
        JSONArray movieArray = movieListJson.getJSONArray(MDB_RESULTS);

        String[] imageUrls = new String[movieArray.length()];
        for (int i = 0; i < movieArray.length(); i++){
            JSONObject movie = movieArray.getJSONObject(i);
            imageUrls[i] = formatImageURL(movie.getString(MDB_MOVIE_POSTER));
        }

        return imageUrls;
    }

    // Buuilt image url with base url + default image size 'w300'
    public String formatImageURL(String imageUrl){
        final String BASE_URL = "http://image.tmdb.org/t/p/";
        final String IMG_SIZE = "w500";
        return BASE_URL+IMG_SIZE+imageUrl;
    }

    public void getMovieList(){
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        String sortBy = preferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_default));
        new FetchMoviesList().execute(sortBy);
    }
}
