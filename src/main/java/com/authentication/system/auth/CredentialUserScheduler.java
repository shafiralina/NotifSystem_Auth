package com.authentication.system.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// This class for making a scheduler for CredentialUserDatabase
// so every new data will readable even we didn't restart the program

@Component
public class CredentialUserScheduler {

	
	@Autowired
	private CredentialUserDatabase data;
	
	@Scheduled(fixedRate = 5000)
	@Async("transactionPoolExecutor")
	public void scheduler() {
		data.credentialData();
		System.out.println("Get data from database");
	}
}
