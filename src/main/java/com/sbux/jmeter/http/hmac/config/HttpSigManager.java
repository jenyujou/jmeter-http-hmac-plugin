package com.sbux.jmeter.http.hmac.config;

import org.apache.http.util.Args;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpSigManager extends ConfigTestElement {
	  private static final String lineSeparator ="\n";
    private static final Logger log = LoggingManager.getLoggerForClass();
    private static final String DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String TIME_ZONE = "GMT";

    public static final String KEY_ID = "HttpSigManager.KeyId";
    public static final String SECRET = "HttpSigManager.Secret";
    public static final String MAC_ALGORITHM = "HttpSigManager.Mac_Algorithm";
    public static final String HEADERS = "HttpSigManager.Headers";


    public List<Header> getHeaders(String body, String method, String path) {
        validate();

        String date = getDate();
        String digest = hash(body.getBytes(Charset.defaultCharset()));
        String requestTarget = method.toLowerCase() + " " + path;
        byte[] message = getMessage(date, digest, requestTarget);

        List<Header> headers = new ArrayList<>();
        headers.add(new Header("(request-target)", requestTarget));
        headers.add(new Header("Digest", digest));
        headers.add(new Header("Date", date));
        headers.add(new Header("Authorization", getAuthorization(message)));

        return headers;
    }

    private void validate() {
        Args.notEmpty(getPropertyAsString(KEY_ID), KEY_ID) ;
        Args.notEmpty(getPropertyAsString(SECRET),SECRET);
        Args.notEmpty(getPropertyAsString(MAC_ALGORITHM),MAC_ALGORITHM);
        Args.notEmpty(getPropertyAsString(HEADERS),HEADERS);
    }

    private String getAuthorization(byte[] message) {
        final String WRAP = "\"";

        StringBuilder sb = new StringBuilder();
        sb.append("Signature ");
        sb.append("keyId=");
        sb.append(WRAP);
        sb.append(getPropertyAsString(KEY_ID));
        sb.append(WRAP);
        sb.append(",");
        sb.append("algorithm=");
        sb.append(WRAP);
        sb.append(getPropertyAsString(MAC_ALGORITHM));
        sb.append(WRAP);
        sb.append(",");
        sb.append("headers=");
        sb.append(WRAP);
        sb.append(getPropertyAsString(HEADERS));
        sb.append(WRAP);
        sb.append(",");
        sb.append("signature=");
        sb.append(WRAP);
        sb.append(sign(message));
        sb.append(WRAP);

        return sb.toString();
    }

    private byte[] getMessage(String date, String digest, String requestTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append("(request-target): ");
        sb.append(requestTarget);
        sb.append(lineSeparator);
        sb.append("date: ");
        sb.append(date);
        sb.append(lineSeparator);
        sb.append("digest: ");
        sb.append(digest);

        return sb.toString().getBytes(Charset.defaultCharset());
    }

    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat(DATE_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        return df.format(Calendar.getInstance().getTime());
    }

    private String hash(byte[] body) {
        MessageDigest md = getMessageDigest();
        byte[] hash = Base64.getEncoder().encode(md.digest(body));
        return DigestAlgorithm.SHA256.value + "=" + new String(hash);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(DigestAlgorithm.SHA256.value);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Mac getMessageAuthenticationCode() {
        try {
            Mac mac = Mac.getInstance(MacAlgorithm.HmacSHA256.value);
            mac.init(getSecretKeySpec());
            return mac;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        byte[] secret = getPropertyAsString(SECRET).getBytes();
        return new SecretKeySpec(secret, MacAlgorithm.HmacSHA256.value);
    }

    private String sign(byte[] message) {
        Mac mac = getMessageAuthenticationCode();
        return new String(Base64.getEncoder().encode(mac.doFinal(message)));
    }

    private enum DigestAlgorithm {
        SHA256("SHA-256");

        private final String value;

        DigestAlgorithm(String value) {
            this.value = value;
        }
    }

    private enum MacAlgorithm {
        HmacMD5("HmacMD5"), HmacSHA1("HmacSHA1"), HmacSHA256("HmacSHA256");

        private final String value;

        MacAlgorithm(String value) {
            this.value = value;
        }
    }
}
