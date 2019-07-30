package com.authentication.system.message;

public class BaseResponse {

	public BaseResponse(String rc, String message, String token) {
		super();
		this.rc = rc;
		this.message = message;
		this.token = token;
	}

	private String rc;
	private String message;
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getRc() {
		return rc;
	}

	public void setRc(String rc) {
		this.rc = rc;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
