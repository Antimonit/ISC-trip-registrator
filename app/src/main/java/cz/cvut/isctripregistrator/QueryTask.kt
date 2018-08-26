package cz.cvut.isctripregistrator

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.json.JSONException
import org.json.JSONObject

import android.os.AsyncTask

class QueryTask(private val queryUrl: String, private val function: Function<JSONObject>) : AsyncTask<String, String, JSONObject>() {

	override fun doInBackground(vararg params: String): JSONObject {
		val client = DefaultHttpClient()
		val response: HttpResponse
		var result: JSONObject

		try {
			val request = HttpPost(queryUrl)
			val pairs = ArrayList<NameValuePair>()
			var i = 0
			while (i < params.size - 1) {
				pairs.add(BasicNameValuePair(params[i], params[i + 1]))
				i += 2
			}
			request.entity = UrlEncodedFormEntity(pairs)

			response = client.execute(request)
			val statusLine = response.statusLine
			if (statusLine.statusCode == HttpStatus.SC_OK) {
				val outputStream = ByteArrayOutputStream()
				response.entity.writeTo(outputStream)
				outputStream.close()
				result = JSONObject(outputStream.toString())
			} else {
				response.entity.content.close()
				throw IOException(statusLine.reasonPhrase)
			}
		} catch (e: IOException) {
			result = generateError(QueryError.ERR_CONNECTION)
			e.printStackTrace()
		} catch (e: JSONException) {
			result = generateError(QueryError.ERR_INTERNAL)
			e.printStackTrace()
		}

		return result
	}

	override fun onPostExecute(result: JSONObject) {
		super.onPostExecute(result)
		function.execute(result)
	}

	companion object {

		internal fun generateError(error: QueryError): JSONObject {
			val result = JSONObject()
			try {
				result.put(MainActivity.STATUS_JSON_KEY, MainActivity.STATUS_JSON_VALUE_ERROR)
				result.put(MainActivity.TYPE_JSON_KEY, error.type)
			} catch (e: JSONException) {
				// do nothing
			}

			return result
		}
	}
}
