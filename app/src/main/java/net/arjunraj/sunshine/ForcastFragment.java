package net.arjunraj.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ForcastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    View rootView;
    TextView todayWeather,textView_location;
    FrameLayout fl;
    String locationLon,locationLat,location="Pune";
    ImageView imageView_currentWeather;
    String forecastJsonStr = null;
    final String FORECAST_BASE_URL =
            "http://api.openweathermap.org/data/2.5/forecast/daily?";
    final String QUERY_PARAM = "q";
    final String FORMAT_PARAM = "mode";
    final String UNITS_PARAM = "units";
    final String DAYS_PARAM = "cnt";

    public ForcastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);



    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute(location);
            return true;
        }
        if (id == R.id.action_search) {

            Intent i = new Intent(getActivity(),search.class);
            startActivityForResult(i, 42);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
//Get's called when activity started ends.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case 42:
                if(data!=null) {
                    location = data.getStringExtra("newLocation");
                    FetchWeatherTask weatherTask = new FetchWeatherTask();
                    weatherTask.execute(location);
                }
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        todayWeather=(TextView) rootView.findViewById(R.id.current_weather);
        textView_location=(TextView) rootView.findViewById(R.id.textView_location);
        fl = (FrameLayout)rootView.findViewById(R.id.frameLayout);
        imageView_currentWeather = (ImageView)rootView.findViewById(R.id.imageView_currentWeather);
        String[] data = {};
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        locationLon="18.4995778";
        locationLat="73.8440372";
        weatherTask.execute(location);

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forcast,// The name of the layout ID.
                        R.id.list_item_textView, // The ID of the textview to populate.
                        weekForecast);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listView_forcast);
        listView.setAdapter(mForecastAdapter);
        return rootView;
    }



    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private final String LOG_OUTPUT = "LOG_OUT";

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();
            int count=0;
            String[] resultStrs = new String[numDays];

            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;


                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " +"High: "+ high+" Low: "+low;
            }

            for (String s : resultStrs) {

            }
            return resultStrs;

        }


        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;


            String format = "json";
            String units = "metric";
            int numDays = 14;

            try {
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
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
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            String[] splitString;
            textView_location.setText(location.substring(0, 1).toUpperCase()+location.substring(1));
            if (result != null) {
                mForecastAdapter.clear();
                todayWeather.clearComposingText();
                //to set ListView and TextView with weather
                for(String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);

                }

                todayWeather.setText(result[0]);

                splitString=result[0].split("- ");

                if("Rain ".equals(splitString[1])){
                    fl.setBackground(getResources().getDrawable(R.drawable.rain));
                    imageView_currentWeather.setBackground(getResources().getDrawable(R.drawable.rainicon));

                }
                else if("Clear ".equals(splitString[1])) {
                    fl.setBackground(getResources().getDrawable(R.drawable.yellow));
                    imageView_currentWeather.setBackground(getResources().getDrawable(R.drawable.skyisclear));
                }
                else if("Clouds ".equals(splitString[1])) {
                    fl.setBackground(getResources().getDrawable(R.drawable.blue));
                    imageView_currentWeather.setBackground(getResources().getDrawable(R.drawable.clouds));
                }
                else{
                    fl.setBackground(getResources().getDrawable(R.drawable.blue));
                }


                //fl.setBackground(getResources().getDrawable(R.drawable.blue));

            }
        }
    }
}