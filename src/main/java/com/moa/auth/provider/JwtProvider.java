package com.moa.auth.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.moa.dto.auth.TokenResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {

	private static final String AUTHORITIES_KEY = "auth";
	private static final String BEARER_TYPE = "Bearer";

	private final SecretKey secretKey;
	private final long accessTokenExpirationMillis;
	private final long refreshTokenExpirationMillis;

	public JwtProvider(@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiration-millis}") long accessTokenExpirationMillis,
			@Value("${jwt.refresh-token-expiration-millis}") long refreshTokenExpirationMillis) {

		byte[] keyBytes = Decoders.BASE64.decode(secret);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
		this.accessTokenExpirationMillis = accessTokenExpirationMillis;
		this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
	}

	public TokenResponse generateToken(Authentication authentication) {

		String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));

		long now = (new Date()).getTime();
		Date accessTokenExpiresIn = new Date(now + accessTokenExpirationMillis);
		Date refreshTokenExpiresIn = new Date(now + refreshTokenExpirationMillis);

		String accessToken = Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities)
				.setExpiration(accessTokenExpiresIn).signWith(secretKey, SignatureAlgorithm.HS256).compact();

		String refreshToken = Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, authorities)
				.setExpiration(refreshTokenExpiresIn).signWith(secretKey, SignatureAlgorithm.HS256).compact();

		return TokenResponse.builder().grantType(BEARER_TYPE).accessToken(accessToken).refreshToken(refreshToken)
				.accessTokenExpiresIn(accessTokenExpiresIn.getTime()).build();
	}

	public TokenResponse refresh(String refreshToken) {

		Claims claims = parseClaims(refreshToken);

		String userId = claims.getSubject();
		String authorities = claims.get(AUTHORITIES_KEY, String.class);

		if (userId == null || userId.isBlank()) {
			throw new RuntimeException("리프레시 토큰에 사용자 정보가 없습니다.");
		}

		long now = (new Date()).getTime();
		Date accessTokenExpiresIn = new Date(now + accessTokenExpirationMillis);
		Date newRefreshTokenExpiresIn = new Date(now + refreshTokenExpirationMillis);

		String newAccessToken = Jwts.builder().setSubject(userId).claim(AUTHORITIES_KEY, authorities)
				.setExpiration(accessTokenExpiresIn).signWith(secretKey, SignatureAlgorithm.HS256).compact();

		String newRefreshToken = Jwts.builder().setSubject(userId).claim(AUTHORITIES_KEY, authorities)
				.setExpiration(newRefreshTokenExpiresIn).signWith(secretKey, SignatureAlgorithm.HS256).compact();

		return TokenResponse.builder().grantType(BEARER_TYPE).accessToken(newAccessToken).refreshToken(newRefreshToken)
				.accessTokenExpiresIn(accessTokenExpiresIn.getTime()).build();
	}

	public Authentication getAuthentication(String accessToken) {
		Claims claims = parseClaims(accessToken);

		if (claims.get(AUTHORITIES_KEY) == null) {
			throw new RuntimeException("Authority information not found in the token.");
		}

		Collection<? extends GrantedAuthority> authorities = Arrays
				.stream(claims.get(AUTHORITIES_KEY).toString().split(",")).map(SimpleGrantedAuthority::new).toList();

		UserDetails principal = new User(claims.getSubject(), "", authorities);

		return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
			return true;
		} catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
			log.info("Invalid JWT Signature", e);
		} catch (ExpiredJwtException e) {
			log.info("Expired JWT Token", e);
		} catch (UnsupportedJwtException e) {
			log.info("Unsupported JWT Token", e);
		} catch (IllegalArgumentException e) {
			log.info("JWT claims string is empty", e);
		}
		return false;
	}

	public Claims parseClaims(String accessToken) {
		try {
			return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(accessToken).getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}