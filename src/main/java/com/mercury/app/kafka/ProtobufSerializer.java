package com.mercury.app.kafka;

import org.apache.kafka.common.serialization.Serializer;

import com.google.protobuf.Message;

public class ProtobufSerializer<T extends Message> extends Adapter implements Serializer<T> {

	@Override
	public byte[] serialize(final String topic, final T data) {
		return data.toByteArray();
	}

}
