package cz.cvut.isctripregistrator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

public class QueryTask extends AsyncTask<String, String, JSONObject> {
    private final Function<JSONObject> function;
    private final String queryUrl;

    public QueryTask(String queryUrl, Function<JSONObject> function) {
        this.function = function;
        this.queryUrl = queryUrl;
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        JSONObject result;

        try {
            HttpPost request = new HttpPost(queryUrl);
            List<NameValuePair> pairs = new ArrayList<>();
            for (int i = 0; i < params.length - 1; i += 2) {
                pairs.add(new BasicNameValuePair(params[i], params[i + 1]));
            }
            request.setEntity(new UrlEncodedFormEntity(pairs));

            response = client.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                response.getEntity().writeTo(outputStream);
                outputStream.close();
                result = new JSONObject(outputStream.toString());
            } else {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            result = generateError(QueryError.ERR_CONNECTION);
            e.printStackTrace();
        } catch (JSONException e) {
            result = generateError(QueryError.ERR_INTERNAL);
            e.printStackTrace();
        }

        return result;
    }

    static JSONObject generateError(QueryError error) {
        JSONObject result = new JSONObject();
        try {
            result.put(MainActivity.STATUS_JSON_KEY, MainActivity.STATUS_JSON_VALUE_ERROR);
            result.put(MainActivity.TYPE_JSON_KEY, error.getType());
        } catch (JSONException e) {
            // do nothing
        }
        return result;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        super.onPostExecute(result);
        function.execute(result);
    }
}
