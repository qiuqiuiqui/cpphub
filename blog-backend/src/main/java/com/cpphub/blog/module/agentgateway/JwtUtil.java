package com.cpphub.blog.module.agentgateway;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * Minimal stateless JWT (HS256) implementation for agent authentication.
 * Format: base64url(header).base64url(payload).base64url(HMAC-SHA256).
 *
 * MVP-only: shared secret comes from env var CPPHUB_AGENT_SECRET, defaulting to a placeholder.
 */
public final class JwtUtil {
  private static final String SECRET =
      System.getenv().getOrDefault("CPPHUB_AGENT_SECRET", "cpphub-mvp-secret-change-me-please");

  private JwtUtil() {}

  public static String mint(String deviceId, Long userId, Duration ttl) {
    String header = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    long exp = Instant.now().plus(ttl).getEpochSecond();
    String payload =
        b64("{\"deviceId\":\"" + deviceId + "\",\"userId\":" + userId + ",\"exp\":" + exp + "}");
    String sig = b64(hmacRaw(header + "." + payload));
    return header + "." + payload + "." + sig;
  }

  public static Parsed parse(String token) {
    if (token == null)
      return null;
    String[] parts = token.split("\\.");
    if (parts.length != 3)
      return null;
    if (!b64(hmacRaw(parts[0] + "." + parts[1])).equals(parts[2]))
      return null;
    try {
      String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      Parsed p = new Parsed();
      p.deviceId = extract(json, "deviceId");
      String uid = extract(json, "userId");
      p.userId = uid == null ? null : Long.parseLong(uid);
      String e = extract(json, "exp");
      p.exp = e == null ? 0 : Long.parseLong(e);
      if (p.exp > 0 && Instant.now().getEpochSecond() > p.exp)
        return null;
      return p;
    } catch (Exception ex) {
      return null;
    }
  }

  private static String extract(String json, String key) {
    String needle = "\"" + key + "\":";
    int i = json.indexOf(needle);
    if (i < 0)
      return null;
    int v = i + needle.length();
    if (v >= json.length())
      return null;
    if (json.charAt(v) == '"') {
      int end = json.indexOf('"', v + 1);
      if (end < 0)
        return null;
      return json.substring(v + 1, end);
    }
    int end = v;
    while (end < json.length() && "0123456789-".indexOf(json.charAt(end)) >= 0) end++;
    return json.substring(v, end);
  }

  private static String b64(String s) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
        s.getBytes(StandardCharsets.UTF_8));
  }

  private static String b64(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static byte[] hmacRaw(String s) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(s.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      return new byte[0];
    }
  }

  public static class Parsed {
    public String deviceId;
    public Long userId;
    public long exp;
  }
}
