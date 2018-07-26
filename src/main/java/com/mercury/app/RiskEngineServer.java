package com.mercury.app;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.sql.SQLException;

import org.jooq.Configuration;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder;
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.mercury.app.repository.BalanceRepository;
import com.mercury.app.repository.DBConnector;
import com.mercury.app.repository.DBLoad;
import com.mercury.app.repository.OrderRepository;
import com.mercury.app.service.RiskEngineService;
import com.mercury.app.service.SettlementService;
import com.mercury.app.service.TestRiskEngineService;

public class RiskEngineServer {

	private static final Logger LOG = LoggerFactory.getLogger(RiskEngineServer.class);

	private Server server;

	private SettlementService settlementConsumer;

	private DBConnector connector;

	private void start(String kafkaServer) throws IOException, CertificateException {
		connector = new DBConnector();
		connector.connect();

		final Configuration dbConfiguration = new DefaultConfiguration().set(connector.getConnection());
		dbConfiguration.set(new ThreadLocalTransactionProvider(dbConfiguration.connectionProvider()));

		final BalanceRepository balanceRepository = new BalanceRepository(dbConfiguration);
		final OrderRepository orderRepository = new OrderRepository(dbConfiguration);

		final DBLoad dbLoader = new DBLoad(balanceRepository);
		dbLoader.loadBalanceData();

		final RiskEngineService riskEngineService = new RiskEngineService(balanceRepository, orderRepository);

		settlementConsumer = new SettlementService(riskEngineService, kafkaServer);

		ServerBuilder sb = new ServerBuilder();

		sb.http(8080).https(8443).tlsSelfSigned();

		sb.service("/", (ctx, req) -> HttpResponse.of("Risk Engine"));

		TestRiskEngineService testService = new TestRiskEngineService(settlementConsumer, balanceRepository);

		sb.service(new GrpcServiceBuilder().addService(riskEngineService).addService(testService)
				.supportedSerializationFormats(GrpcSerializationFormats.values()).enableUnframedRequests(true).build());

		sb.serviceUnder("/docs", new DocService());
		sb.service("/health", new HttpHealthCheckService());
		sb.decorator(LoggingService.newDecorator());

		server = sb.build();

		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

		server.start().join();
	}

	private void stop() {
		if (server != null) {
			LOG.error("Shutting down gRPC server since JVM is shutting down");

			server.stop();
			settlementConsumer.done();
			try {
				connector.close();
			} catch (SQLException e) {
				LOG.error("Faild to cloase connection", e);
			}

			server = null;
			settlementConsumer = null;
			connector = null;
			LOG.error("Server shut down...");
		}
	}

	public static void main(String[] args) throws IOException, CertificateException {
		LOG.info("Starting Risk Engine...");
		String kafkaServer = "localhost:9092";
		if (args.length > 0) {
			kafkaServer = args[0];
		}
		final RiskEngineServer server = new RiskEngineServer();
		server.start(kafkaServer);
	}

}
