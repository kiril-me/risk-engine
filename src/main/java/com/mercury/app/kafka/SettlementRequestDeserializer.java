package com.mercury.app.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mercury.proto.SettlementRequest;

public class SettlementRequestDeserializer extends Adapter implements Deserializer<SettlementRequest> {

	private static final Logger LOG = LoggerFactory.getLogger(SettlementRequestDeserializer.class);

	@Override
	public SettlementRequest deserialize(String topic, byte[] data) {
		try {
			return SettlementRequest.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			LOG.error("Received unparseable message", e);
			throw new RuntimeException("Received unparseable message " + e.getMessage(), e);
		}

	}

}
