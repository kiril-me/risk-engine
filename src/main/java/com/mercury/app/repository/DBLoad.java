package com.mercury.app.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Objects;

import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercury.app.db.public_.Sequences;
import com.mercury.app.db.public_.tables.pojos.Balance;

public class DBLoad {
	private static final Logger LOG = LoggerFactory.getLogger(DBLoad.class);

	private final BalanceRepository balanceRepository;

	public DBLoad(BalanceRepository balanceRepository) {
		this.balanceRepository = Objects.requireNonNull(balanceRepository);
	}

	public void loadBalanceData() throws IOException {
		long count = balanceRepository.count();
		if(count > 0) {
			return;
		}
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("risk-engine-test-data-set.csv");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = reader.readLine();
		if (line != null) {
			final String[] header = line.toUpperCase().split(",");
			line = reader.readLine(); // skip first line
			while (line != null) {
				parseLine(line, header);
				line = reader.readLine();
			}
		}
		is.close();
	}

	private void parseLine(final String line, final String[] header) {
		String[] parts = line.split(",");
		if (parts.length == 6) {
			Long userId = null;
			try {
				userId = Long.parseLong(parts[0]);
			} catch (NumberFormatException e) {
				LOG.error("User id must be the long {}", parts[0]);
			}
			if (userId != null) {
				for (int i = 1; i < parts.length; i++) {
					try {
						BigDecimal amount = new BigDecimal(parts[i]);
						final Long balanceId = DSL.using(balanceRepository.configuration()).nextval(Sequences.S_BALNCE_ID);
						final Balance balance = new Balance(balanceId, header[i].toUpperCase(), userId, amount, BigDecimal.ZERO);
						balanceRepository.insert(balance);
					} catch (NumberFormatException e) {
						LOG.error("Could not read currence {} amount {} for user {}", header[i], parts[i], userId);
					}
				}
			}
		} else {
			LOG.error("The line has wrong format: " + line);
		}
	}
}
