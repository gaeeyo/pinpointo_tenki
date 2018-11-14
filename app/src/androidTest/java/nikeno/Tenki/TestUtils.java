package nikeno.Tenki;

import androidx.test.InstrumentationRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestUtils {

    public static String readAssetFile(String filename) throws IOException {
        return readString(InstrumentationRegistry.getContext().getAssets().open(filename));
    }

    public static String readString(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int                   c;
            while ((c = is.read()) != -1) out.write(c);

            return new String(out.toByteArray(), "utf-8");
        } finally {
            is.close();
        }
    }
}
