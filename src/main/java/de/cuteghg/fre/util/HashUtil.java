package de.cuteghg.fre.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class HashUtil {
    private HashUtil() {
    }

    public static String sha1Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Unable to create SHA-1", e);
        }
    }

    public static byte[] sha1BytesFromHex(String hex) {
        if (hex == null || hex.length() != 40) {
            return new byte[0];
        }
        byte[] out = new byte[20];
        for (int i = 0; i < out.length; i++) {
            int idx = i * 2;
            out[i] = (byte) Integer.parseInt(hex.substring(idx, idx + 2), 16);
        }
        return out;
    }
}
