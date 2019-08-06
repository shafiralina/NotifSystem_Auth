package com.authentication.system.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.authentication.system.common.JwtConfig;
import com.authentication.system.message.BaseResponse;
import com.authentication.system.message.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUsernameAndPasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	// We use auth manager to validate the user credentials
	private AuthenticationManager authManager;

	@Autowired
	private final JwtConfig jwtConfig;

	private int time;
	private String userId;

	private Gson gson = new Gson();
	
	public JwtUsernameAndPasswordAuthenticationFilter(AuthenticationManager authManager, JwtConfig jwtConfig) {
		this.authManager = authManager;
		this.jwtConfig = jwtConfig;

		// By default, UsernamePasswordAuthenticationFilter listens to "/login" path.
		// In our case, we use "/auth". So, we need to override the defaults.

		this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher(jwtConfig.getUri(), "POST"));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {

		try {

			// 1. Get credentials from request
			UserCredentialsNew credsNew = new ObjectMapper().readValue(request.getInputStream(), UserCredentialsNew.class);

			// 2. Create auth object (contains credentials) which will be used by auth
			// manager
			UserCredentials creds = new UserCredentials();
			creds.setPassword("12345");
			creds.setUsername(credsNew.getUsername());
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(creds.getUsername(),
					creds.getPassword(), Collections.emptyList());

			this.userId = creds.getUsername();

			// 3. Authentication manager authenticate the user, and use
			// UserDetialsServiceImpl::loadUserByUsername() method to load the user.
			return authManager.authenticate(authToken);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}	
	}

	// Upon successful authentication, generate a token.
	// The 'auth' passed to successfulAuthentication() is the current authenticated
	// user.
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication auth) throws IOException, ServletException {
		
		// Set token's expiration time  depends on its channel
		String channel = request.getHeader("Channel");
		System.out.println("Channel = " + channel);

		if (channel.equals("MB")) {
			time = jwtConfig.getExpiration1();
		} else {
			time = jwtConfig.getExpiration2();
		}
		
		
		Long now = System.currentTimeMillis();
		String token = Jwts.builder().setSubject(auth.getName())
				// Convert to list of strings.
				// This is important because it affects the way we get them back in the Gateway.
				.claim("authorities",
						auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
				.setIssuedAt(new Date(now)).setExpiration(new Date(now + time * 1000)) // in milliseconds
				.signWith(SignatureAlgorithm.HS512, jwtConfig.getSecret().getBytes()).compact();
		System.out.println("time = "+time);
		dataCredential(userId, token);
		// Add token to header
		response.addHeader(jwtConfig.getHeader(), jwtConfig.getPrefix() + " " + token);
		
		
		// Add response body
		BaseResponse body = new BaseResponse(ResponseCode.VALID_RESPONSE, "Success", token);
		String responses = this.gson.toJson(body);
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		out.print(responses);
		out.flush();
	}

	@Async("transactionPoolExecutor")
	public void dataCredential(String userId, String token) {
		String result = "";
		HttpClient client = new HttpClient();
		GetMethod getMethod = new GetMethod("http://localhost:3000/user/" + userId + "/" + token);
		try {
			client.executeMethod(getMethod);
			System.out.println("USER ID = " + userId);
			System.out.println("TOKEN = " + token);
		} catch (Exception e) {
			logger.error(e);
		} finally {
			getMethod.releaseConnection();
		}
	}

	// A (temporary) class just to represent the user credentials
	private static class UserCredentials {
		private String username, password;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
	
	private static class UserCredentialsNew {
		private String username;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

	}
}