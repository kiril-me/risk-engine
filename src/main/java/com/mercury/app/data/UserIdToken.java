package com.mercury.app.data;

import java.io.Serializable;
import java.util.Objects;

public class UserIdToken implements Serializable {
	/**
	 * Class version.
	 */
	private static final long serialVersionUID = 1L;

	private final Long userId;

	private final String token;

	public UserIdToken(Long userId, String token) {
		this.userId = Objects.requireNonNull(userId);
		this.token = Objects.requireNonNull(token);
	}

	public static UserIdToken of(Long userId, String token) {
		return new UserIdToken(userId, token);
	}

	public Long getUserId() {
		return userId;
	}

	public String getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserIdToken other = (UserIdToken) obj;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
