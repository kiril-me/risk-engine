package com.mercury.app.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnector {

	private static final Logger LOG = LoggerFactory.getLogger(DBConnector.class);

	private volatile Connection connection;
	private Properties properties;

	public void connect() {
		if (connection == null) {
			synchronized (this) {
				if (connection == null) {
					LOG.info("Creating db connection...");

					Properties props = properties();
					String driver = props.getProperty("db.driver");
					String jdbcUrl = props.getProperty("db.url");
					String username = props.getProperty("db.username");
					String password = props.getProperty("db.password");

					try {
						Class.forName(driver);
						connection = DriverManager.getConnection(jdbcUrl, username, password);
					} catch (Exception e) {
						LOG.error("Failed to create DB connection", e);
						throw new RuntimeException(e);
					}
					LOG.info("DB connection done");

					loadData();
				}
			}
		}
	}

	private void loadData() {
		// TODO
	}

	private Properties properties() {
		if (properties == null) {
			try {
				properties = new Properties();
				properties.load(DBConnector.class.getResourceAsStream("/config.properties"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return properties;
	}
	
	public Connection getConnection() {
		return connection;
	}
//
//	public void start() {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void finish() {
//		// TODO Auto-generated method stub
//		
//	}
//
	public void close() throws SQLException {
		if(connection != null) {
			connection.close();
		}
	}
}
