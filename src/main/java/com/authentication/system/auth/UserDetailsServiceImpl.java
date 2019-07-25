package com.authentication.system.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service // It has to be annotated with @Service.
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private BCryptPasswordEncoder encoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		// call API CredentialUser to send credential user to NotifSystem
		String result = "";
		HttpClient client = new HttpClient();
		GetMethod getMethod = new GetMethod("http://localhost:8100/credential/user");
		CredentialUserResponse credential = new CredentialUserResponse();

		try {
			client.executeMethod(getMethod);
			result = getMethod.getResponseBodyAsString();
		} catch (Exception e) {
		} finally {
			getMethod.releaseConnection();
		}

		HashMap<String, Object> result1 = new HashMap<>();
		try {
			result1 = new ObjectMapper().readValue(result, HashMap.class);
			credential.setData(result1.get("data"));

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Add userCredential to appUser
		ArrayList<LinkedHashMap<String, Object>> data = credential.getData();
		final List<AppUser> userCredential = new ArrayList<UserDetailsServiceImpl.AppUser>();
		
		for (int i = 0; i < data.size(); i++) {
			AppUser user = new AppUser();
			user.setId(data.get(i).get("id"));
			user.setUsername(data.get(i).get("userId"));
			user.setPassword(encoder.encode(data.get(i).get("password").toString()));
			user.setRole(data.get(i).get("role"));
			userCredential.add(user);
		}
		System.out.println(userCredential);

		for (AppUser appUser : userCredential) {
			if (appUser.getUsername().equals(username)) {

				// Remember that Spring needs roles to be in this format: "ROLE_" + userRole
				// (i.e. "ROLE_ADMIN")
				// So, we need to set it to that format, so we can verify and compare roles
				// (i.e. hasRole("ADMIN")).
				List<GrantedAuthority> grantedAuthorities = AuthorityUtils
						.commaSeparatedStringToAuthorityList("ROLE_" + appUser.getRole());

				// The "User" class is provided by Spring and represents a model class for user
				// to be returned by UserDetailsService
				// And used by auth manager to verify and check user authentication.
				return new User(appUser.getUsername(), appUser.getPassword(), grantedAuthorities);
			}
		}

		// If user not found. Throw this exception.
		throw new UsernameNotFoundException("Username: " + username + " not found");
	}

	// A (temporary) class represent the user saved in the database.
	private static class AppUser {
		private Integer id;
		private String username, password;
		private String role;

		public AppUser() {

		}

		public void setId(Object object) {
			this.id = (Integer) object;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(Object object) {
			this.username = (String) object;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(Object object) {
			this.password = (String) object;
		}

		public String getRole() {
			return role;
		}

		public void setRole(Object object) {
			this.role = (String) object;
		}
	}
}