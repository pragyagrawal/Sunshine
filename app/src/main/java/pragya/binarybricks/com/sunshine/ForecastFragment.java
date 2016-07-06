package pragya.binarybricks.com.sunshine;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import pragya.binarybricks.com.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ForecastFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    ListView forecastListView;
    List<String> forecastList;
    ForecastAdapter mForecastAdapter;
    String inputLocation;
    private static final int FORECAST_LOADER = 0;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    private void getWeatherForecastDataFromUserPreference() {
//        SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
//        inputLocation = sharedPref.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default_value));
//        FetchWeatherForecast fetchWeatherForecast = new FetchWeatherForecast();
//        if(!inputLocation.isEmpty()) {
//            fetchWeatherForecast.execute(inputLocation);
//        }
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View mainFragmentView= inflater.inflate(R.layout.fragment_main, container, false);
         forecastListView = (ListView) mainFragmentView.findViewById(R.id.list_view_forecast_textview);
        forecastListView.setAdapter(mForecastAdapter);

        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Intent detailWeatherIntent = new Intent(getActivity(), DetailWeatherActivity.class);
                    detailWeatherIntent.setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting,
                            cursor.getLong(COL_WEATHER_DATE)));
                    startActivity(detailWeatherIntent);
                }
            }

        });
        return mainFragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle){
        String locationSetting = Utility.getPreferredLocation(getActivity());

        //Sort order : ascending by date
        String sortOrder= WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherLocationUri= WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate
                (locationSetting, System.currentTimeMillis());

        return new android.support.v4.content.CursorLoader(
                getActivity(),
                weatherLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

//
//    private  String convertCursorRowToUXFormat(Cursor cursor) {
//        String highAndLow= formatHighLows(
//                cursor.getDouble(COL_WEATHER_MAX_TEMP),
//                cursor.getDouble(COL_WEATHER_MIN_TEMP));
//
//        return Utility.formatDate(cursor.getLong(COL_WEATHER_DATE)) + "-" + cursor.getString(COL_WEATHER_DESC)
//                + "-" + highAndLow;
    }
//    public class FetchWeatherForecast extends AsyncTask<String,Integer,String[]>{
//
//        @Override
//        protected void onPreExecute() {
//
//            Toast refreshMessage= Toast.makeText(getActivity(),"The data is getting refreshed",Toast.LENGTH_SHORT);
//            refreshMessage.show();
//        }
//
//        @Override
//        protected String[] doInBackground(String... postalCode) {
//            return getWeatherDataFromServer(postalCode[0]);
//        }
//
//        @Override
//        protected void onPostExecute(String[] strings) {
//            forecastList= Arrays.asList(strings);
//
//            mForecastAdapter = new ArrayAdapter<String>(
//
//                    getActivity(),
//                    R.layout.list_item_forecast,
//                    R.id.list_view_forecast_textview,
//                    forecastList
//            );
//            forecastListView.setAdapter(mForecastAdapter);
//            }
//    }
//    private String[] getWeatherDataFromServer(String postalCode){
//// These two need to be declared outside the try/catch
//// so that they can be closed in the finally block.
//        HttpURLConnection urlConnection = null;
//        BufferedReader reader = null;
//
//// Will contain the raw JSON response as a string.
//        String forecastJsonStr = null;
//
//        try {
//            // Construct the URL for the OpenWeatherMap query
//            // Possible parameters are available at OWM's forecast API page, at
//            // http://openweathermap.org/API#forecast
//            Uri.Builder urlBuilder = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily").buildUpon();
//            urlBuilder.appendQueryParameter("zip",postalCode);
//            urlBuilder.appendQueryParameter("units","metric");
//            urlBuilder.appendQueryParameter("cnt","7");
//            urlBuilder.appendQueryParameter("appid","37604c2b6669a395923bd1105bf97b09");
//
//            URL url = new URL(urlBuilder.build().toString());
//
//            // Create the request to OpenWeatherMap, and open the connection
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("GET");
//            urlConnection.connect();
//
//            // Read the input stream into a String
//            InputStream inputStream = urlConnection.getInputStream();
//            StringBuffer buffer = new StringBuffer();
//            if (inputStream == null) {
//                // Nothing to do.
//                forecastJsonStr = null;
//            }
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                // But it does make debugging a *lot* easier if you print out the completed
//                // buffer for debugging.
//                buffer.append(line + "\n");
//            }
//
//            if (buffer.length() == 0) {
//                // Stream was empty.  No point in parsing.
//                forecastJsonStr = null;
//            }
//            forecastJsonStr = buffer.toString();
//            try {
//                return getWeatherDataFromJson(forecastJsonStr,7);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//           // Log.v("LOG_TAG","Forecast JSON String:" + forecastJsonStr);
//
//        } catch (IOException e) {
//            Log.e("PlaceholderFragment", "Error ", e);
//            // If the code didn't successfully get the weather data, there's no point in attempting
//            // to parse it.
//            forecastJsonStr = null;
//        } finally{
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                } catch (final IOException e) {
//                    Log.e("PlaceholderFragment", "Error closing stream", e);
//                }
//            }
//        }
//        return null;
//    }
//
//    /* The date/time conversion code is going to be moved outside the asynctask later,
// * so for convenience we're breaking it out into its own method now.
// */
//    private String getReadableDateString(long time){
//        // Because the API returns a unix timestamp (measured in seconds),
//        // it must be converted to milliseconds in order to be converted to valid date.
//        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
//        return shortenedDateFormat.format(time);
//    }
//
//    /**
//     * Prepare the weather high/lows for presentation.
//     */
//    private String formatHighLows(double high, double low) {
//        // For presentation, assume the user doesn't care about tenths of a degree.
//        long roundedHigh = Math.round(high);
//        long roundedLow = Math.round(low);
//
//        String highLowStr = roundedHigh + "/" + roundedLow;
//        return highLowStr;
//    }
//
//    /**
//     * Take the String representing the complete forecast in JSON Format and
//     * pull out the data we need to construct the Strings needed for the wireframes.
//     *
//     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
//     * into an Object hierarchy for us.
//     */
//    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
//            throws JSONException {
//
//        // These are the names of the JSON objects that need to be extracted.
//        final String OWM_LIST = "list";
//        final String OWM_WEATHER = "weather";
//        final String OWM_TEMPERATURE = "temp";
//        final String OWM_MAX = "max";
//        final String OWM_MIN = "min";
//        final String OWM_DESCRIPTION = "main";
//
//        JSONObject forecastJson = new JSONObject(forecastJsonStr);
//        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
//
//        // OWM returns daily forecasts based upon the local time of the city that is being
//        // asked for, which means that we need to know the GMT offset to translate this data
//        // properly.
//
//        // Since this data is also sent in-order and the first day is always the
//        // current day, we're going to take advantage of that to get a nice
//        // normalized UTC date for all of our weather.
//
//        Time dayTime = new Time();
//        dayTime.setToNow();
//
//        // we start at the day returned by local time. Otherwise this is a mess.
//        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
//
//        // now we work exclusively in UTC
//        dayTime = new Time();
//
//        String[] resultStrs = new String[numDays];
//        for(int i = 0; i < weatherArray.length(); i++) {
//            // For now, using the format "Day, description, hi/low"
//            String day;
//            String description;
//            String highAndLow;
//
//            // Get the JSON object representing the day
//            JSONObject dayForecast = weatherArray.getJSONObject(i);
//
//            // The date/time is returned as a long.  We need to convert that
//            // into something human-readable, since most people won't read "1400356800" as
//            // "this saturday".
//            long dateTime;
//            // Cheating to convert this to UTC time, which is what we want anyhow
//            dateTime = dayTime.setJulianDay(julianStartDay+i);
//            day = getReadableDateString(dateTime);
//
//            // description is in a child array called "weather", which is 1 element long.
//            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
//            description = weatherObject.getString(OWM_DESCRIPTION);
//
//            // Temperatures are in a child object called "temp".  Try not to name variables
//            // "temp" when working with temperature.  It confuses everybody.
//            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//            double high = temperatureObject.getDouble(OWM_MAX);
//            double low = temperatureObject.getDouble(OWM_MIN);
//            SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
//            int inputTemperatureUnit= Integer.parseInt(sharedPref.getString(getString(R.string.pref_units_key),"0"));
//            if(inputTemperatureUnit==1) {
//                high=(high*1.8)+32;
//                low =(low*1.8)+32;
//                highAndLow = formatHighLows(high, low) + "F";
//            }
//            else {
//                highAndLow = formatHighLows(high, low) + "C";
//            }
//            resultStrs[i] = day + " - " + description + " - " + highAndLow;
//        }
//
//        return resultStrs;
//    }
//}
