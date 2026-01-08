package com.myqyl.aitradex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.myqyl.aitradex.config.AlphaVantageProperties;
import com.myqyl.aitradex.config.MarketDataProperties;
import com.myqyl.aitradex.etrade.config.EtradeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({MarketDataProperties.class, AlphaVantageProperties.class, EtradeProperties.class})
@EnableScheduling
public class AitradexApplication {

  public static void main(String[] args) {
    SpringApplication.run(AitradexApplication.class, args);
  }
}
