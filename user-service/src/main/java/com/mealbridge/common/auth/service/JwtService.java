package com.mealbridge.common.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

	@Value("${jwt.secretKeyAt}")
	private String secretKeyAtValue;

	@Value("${jwt.secretKeyRt}")
	private String secretKeyRtValue;

	@Value("${jwt.expirationAt}")
	private long expirationAtMillis;

	@Value("${jwt.expirationRt}")
	private long expirationRtMillis;

	private SecretKey accessKey;
	private SecretKey refreshKey;

	@PostConstruct
	public void initKeys() {
		this.accessKey = toSecretKey(secretKeyAtValue);
		this.refreshKey = toSecretKey(secretKeyRtValue);
	}

	private SecretKey toSecretKey(String value) {
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(value);
		} catch (IllegalArgumentException ex) {
			keyBytes = value.getBytes(StandardCharsets.UTF_8);
		}
		return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
	}

	public String createAccessToken(UUID userId, String role, String email) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationAtMillis);
		return Jwts.builder()
				.setSubject(userId.toString())
				.claim("role", role)
				.claim("email", email)
				.setIssuedAt(now)
				.setExpiration(exp)
				.signWith(accessKey, SignatureAlgorithm.HS512)
				.compact();
	}

	public String createRefreshToken(UUID userId, String jti) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationRtMillis);
		return Jwts.builder()
				.setSubject(userId.toString())
				.setId(jti)
				.setIssuedAt(now)
				.setExpiration(exp)
				.signWith(refreshKey, SignatureAlgorithm.HS512)
				.compact();
	}

	public Claims parseAccessToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(accessKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public Claims parseRefreshToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(refreshKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String newJti() {
		return UUID.randomUUID().toString();
	}

	public long getExpirationAtMillis() {
		return expirationAtMillis;
	}

	public long getExpirationRtMillis() {
		return expirationRtMillis;
	}
}
