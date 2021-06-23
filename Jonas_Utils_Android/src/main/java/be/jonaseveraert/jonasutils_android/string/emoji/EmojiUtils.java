package be.jonaseveraert.jonasutils_android.string.emoji;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import be.ksa.voetje.R;
import be.ksa.voetje.util.exception.UnexpectedException;

public abstract class EmojiUtils {
    // NOTE: for creating files -> Environment.getExternalStorageDirectory()

    /**
     * Returns an array of unicode values that can be compared to emoji names using {@link #getUnicodeNames getUnicodeNames}.
     * @param context The context it is executed from.
     * @return An array of unicode values.
     * @throws FileNotFoundException When the JSONFile is missing.
     * @throws IOException When an I/O Exception occurs
     * @throws JSONException if the parse fails or doesn't yield a JSONObject.
     * @throws UnexpectedException if an unexpected error occurs
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static JSONArray getUnicodeValues(Context context) throws FileNotFoundException, IOException, JSONException, UnexpectedException {
        File tempFile = be.ksa.voetje.util.io.FileUtils.rawResourceToTempFile(context, R.raw.unicode_values, "unicode_values.json");
        tempFile.deleteOnExit();

        // https://stackoverflow.com/a/19945493/14874405
        // https://stackoverflow.com/a/16480703/14874405

        // Read json file into text
        StringBuilder jsonText = new StringBuilder();

        // Bufferedreader
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(tempFile));

            // Reading the lines
            String line;
            while ((line = br.readLine()) != null) {
                jsonText.append(line).append(" "); // read lines, all in one line
            }
        } catch (IOException e) {
            throw new IOException("Could not read JSON file", e);
        } finally {
            try {
                if (br != null)
                    br.close();
                else
                    throw new UnexpectedException("Unkown error, the buffered reader is null.");
            } catch (Exception e) {
                throw new UnexpectedException("Unkown error", e);
            }
        }

        JSONObject jsonObject = new JSONObject(jsonText.toString());
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONArray("unicode");
        tempFile.delete();

        return jsonArray;
    }

    // TODO: compare length of the two arrays to make sure nothing went wrong

    /**
     * Returns an array of unicode names that can be compared to unicode values using {@link #getUnicodeValues(Context)}  getUnicodeNames}.
     * @param context The context it is executed from.
     * @return An array of unicode names.
     * @throws FileNotFoundException When the JSONFile is missing.
     * @throws IOException When an I/O Exception occurs
     * @throws JSONException if the parse fails or doesn't yield a JSONObject.
     * @throws UnexpectedException if an unexpected error occurs
     */
    public JSONArray getUnicodeNames(Context context) throws FileNotFoundException, IOException, UnexpectedException, JSONException {
        File tempFile = be.ksa.voetje.util.io.FileUtils.rawResourceToTempFile(context, R.raw.unicode_names, "unicode_values.json");
        tempFile.deleteOnExit();

        // https://stackoverflow.com/a/19945493/14874405
        // https://stackoverflow.com/a/16480703/14874405

        // Read json file into text
        StringBuilder jsonText = new StringBuilder();

        // Bufferedreader
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(tempFile));

            // Reading the lines
            String line;
            while ((line = br.readLine()) != null) {
                jsonText.append(line).append(" "); // read lines, all in one line
            }
        } catch (IOException e) {
            throw new IOException("Could not read JSON file", e);
        } finally {
            try {
                if (br != null)
                    br.close();
                else
                    throw new UnexpectedException("Unkown error, the buffered reader is null.");
            } catch (Exception e) {
                throw new UnexpectedException("Unkown error", e);
            }
        }

        JSONObject jsonObject = new JSONObject(jsonText.toString());
        JSONArray jsonArray = (JSONArray) jsonObject.getJSONArray("unicode");
        tempFile.delete();

        return jsonArray;
    }
}
