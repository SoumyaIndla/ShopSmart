package com.shopsmart.security;
import com.shopsmart.user.ShopUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
@Service public class JwtService {
 private final SecretKey key; private final long expirationMs;
 public JwtService(@Value("${shopsmart.jwt.secret}") String secret,@Value("${shopsmart.jwt.expiration-ms}") long expirationMs){this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.expirationMs=expirationMs;}
 public String create(ShopUser user){long now=System.currentTimeMillis();return Jwts.builder().subject(user.getEmail()).claim("role",user.getRole().name()).claim("tokenVersion",user.getTokenVersion()).issuedAt(new Date(now)).expiration(new Date(now+expirationMs)).signWith(key).compact();}
 public String getEmail(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}
 public int getTokenVersion(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("tokenVersion",Integer.class);}
}
