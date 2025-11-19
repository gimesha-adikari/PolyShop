package com.polyshop.authservice.security;

import org.apache.commons.codec.binary.Base32;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

public final class TotpUtil {
    private static final Base32 base32 = new Base32();
    private static final SecureRandom rnd = new SecureRandom();

    public static String generateBase32Secret() {
        byte[] bytes = new byte[20];
        rnd.nextBytes(bytes);
        return base32.encodeToString(bytes).replace("=", "");
    }

    public static String getOtpAuthUrl(String account, String issuer, String secretBase32) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                urlEncode(issuer), urlEncode(account), secretBase32, urlEncode(issuer));
    }

    private static String urlEncode(String s) {
        return s.replace(" ", "%20");
    }

    public static boolean verifyCode(String secretBase32, int code, long timeWindowSeconds, int skewWindows) {
        long t = Instant.now().getEpochSecond() / timeWindowSeconds;
        for (int i = -skewWindows; i <= skewWindows; i++) {
            if (generateTOTP(secretBase32, t + i) == code) return true;
        }
        return false;
    }

    private static int generateTOTP(String base32Secret, long t) {
        try {
            byte[] key = base32.decode(base32Secret);
            byte[] data = new byte[8];
            long value = t;
            for (int i = 7; i >= 0; --i) {
                data[i] = (byte) (value & 0xff);
                value >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int binary =
                    ((hash[offset] & 0x7f) << 24) |
                            ((hash[offset + 1] & 0xff) << 16) |
                            ((hash[offset + 2] & 0xff) << 8) |
                            (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return otp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
