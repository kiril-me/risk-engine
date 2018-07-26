package com.mercury.app.service;

import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercury.app.kafka.SettlementRequestDeserializer;
import com.mercury.app.kafka.SettlementRequestSerialiazer;
import com.mercury.proto.SettlementRequest;

public class SettlementService {

	private static final Logger LOG = LoggerFactory.getLogger(SettlementService.class);

	private static final String NAME = "risk-engine-";

	private final static String TOPIC = "risk-engine-topic";

//	private final static String BOOTSTRAP_SERVERS = "localhost:9092";

	private static final int WAIT_TIME = 1000;

	private volatile boolean working;

	private ExecutorService executor;

	private Consumer<Long, SettlementRequest> consumer;

	private final RiskEngineService riskEngineService;
	
	private final String servers;

	public SettlementService(RiskEngineService riskEngineService, String servers) {
		this.riskEngineService = Objects.requireNonNull(riskEngineService);
		this.servers =  Objects.requireNonNull(servers);
	}

	private Consumer<Long, SettlementRequest> createConsumer() {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, NAME + "consumer");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SettlementRequestDeserializer.class.getName());

		final Consumer<Long, SettlementRequest> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(TOPIC));
		return consumer;
	}

	/**
	 * Used for testing.
	 * 
	 * @return
	 */
	private Producer<Long, SettlementRequest> createProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, NAME + "producer");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SettlementRequestSerialiazer.class.getName());
		return new KafkaProducer<>(props);
	}

	public void sendSettlement(SettlementRequest request) {
		final Producer<Long, SettlementRequest> producer = createProducer();
		try {

			final ProducerRecord<Long, SettlementRequest> record = new ProducerRecord<>(TOPIC, request.getOrderId(), request);

			producer.send(record).get();

		} catch (InterruptedException e) {
			LOG.error("Producer interrupted", e);
		} catch (ExecutionException e) {
			LOG.error("Execution exception", e);
		} finally {
			producer.flush();
			producer.close();
		}
	}

	public void subscribe() {
		if (consumer != null) {
			return; // don't want to consumer more
		}
		working = true;
		if (consumer == null) {
			consumer = createConsumer();
		}
		executor = Executors.newCachedThreadPool();
		executor.execute(() -> {
			while (working) {
				try {
					consume();
				} catch (Exception e) {
					LOG.error("Could not get the message", e);
				}
			}
			consumer.close();
		});
	}

	private void consume() {
		final ConsumerRecords<Long, SettlementRequest> consumerRecords = consumer.poll(WAIT_TIME);
		if (!consumerRecords.isEmpty()) {
			consumerRecords.forEach(record -> {
				executor.execute(() -> riskEngineService.processSettlement(record.key(), record.value()));
			});
			consumer.commitAsync();
		}
	}

	public void done() {
		working = false;
		executor.shutdown();
	}
}
