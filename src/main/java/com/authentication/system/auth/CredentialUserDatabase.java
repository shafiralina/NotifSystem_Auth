package com.authentication.system.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.authentication.system.auth.UserDetailsServiceImpl.AppUser;
import com.fasterxml.jackson.databind.ObjectMapper;

// This class for calling database CredentialUser in NotifSystem

@Component
@Service
public class CredentialUserDatabase {
	
	@Autowired
	private UserDetailsServiceImpl user;
	
	@Autowired
	private BCryptPasswordEncoder encoder;
	
	private List<AppUser> users;
	
	@EventListener(ApplicationReadyEvent.class)
	public void credentialData() {
	String result = "";
	HttpClient client = new HttpClient();
	GetMethod getMethod = new GetMethod("http://localhost:3000/user");
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

	AppUser a[] = new AppUser[data.size()];
	for (int i = 0; i < data.size(); i++) {
		int parseIntx = Integer.parseInt(data.get(i).get("id").toString());
		String stringx = data.get(i).get("user_id").toString();
		String encodex = encoder.encode(data.get(i).get("password").toString());
		String string2x = data.get(i).get("role").toString();
		AppUser user = new AppUser(parseIntx,stringx,encodex,string2x);
		a[i] = user;
	}
		users = Arrays.asList(a);
	}
	
	public List<AppUser> getData() {
		return users;
	}
	

}
