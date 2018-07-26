package com.mercury.app.kafka;

import java.util.Map;

public abstract class Adapter {
	public void close() {
		// empty	
	}

	public void configure(Map<String, ?> configs, boolean isKey) {
		// no configs
	}
}
