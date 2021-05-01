package com.epodserver;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

import java.io.IOException;
import java.io.Serializable;

public class User implements Serializable {
	public final String validLocationIpAddress;
	public final double latitude, longitude;

	public User(String validLocationIpAddress, double latitude, double longitude) {
		this.validLocationIpAddress = validLocationIpAddress;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof User))
			return false;

		User user = (User) obj;
		return user.validLocationIpAddress.equals(validLocationIpAddress) && user.latitude == latitude && user.longitude == longitude;
	}

	@Override
	public String toString() {
		return String.format(validLocationIpAddress);
	}

	public static class Serializer implements org.mapdb.Serializer<User>, Serializable {

		@Override
		public void serialize(DataOutput2 out, User user) throws IOException {
			out.writeUTF(user.validLocationIpAddress);
			out.writeDouble(user.latitude);
			out.writeDouble(user.longitude);
		}

		@Override
		public User deserialize(DataInput2 in, int available) throws IOException {
			return new User(in.readUTF(), in.readDouble(), in.readDouble());
		}
	}

	public static final Serializer SERIALIZER = new Serializer();
}
