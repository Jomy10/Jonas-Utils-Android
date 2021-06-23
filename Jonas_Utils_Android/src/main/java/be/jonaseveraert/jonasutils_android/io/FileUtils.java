package be.jonaseveraert.jonasutils_android.io;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO: in Jonas' utils for Android
public abstract class FileUtils {
    /**
     * Creates a tempfile from a resource
     * @param context the context from which the method is executed
     * @param id the resource id
     * @param filename the filename (optional)
     * @return the temp file
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular
     *      file, does not exist but cannot be created, or cannot be opened for any other reason
     * @throws IOException if an I/O error occurs
     * @implNote Don't forget to call {@code file.delete()} or {@code file.deleteOnExit()}
     */
    public static File rawResourceToTempFile(Context context, int id, @Nullable String filename) throws FileNotFoundException, IOException {
        InputStream is = context.getResources().openRawResource(id);
        File file;
        if (filename != null)
            file = File.createTempFile("be.ksa.voetje-", "-" + filename);
        else file = File.createTempFile("be.ksa.voetje-", "-resourceFile");
        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(is, outputStream);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        is.close();

        return file;
    }

    public static void copyTo(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
