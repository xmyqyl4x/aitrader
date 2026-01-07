package com.myqyl.aitradex.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data")
public class MarketDataProperties {

  private String defaultSource = "quote-snapshots";
  private Duration cacheTtl = Duration.ofSeconds(30);

  public String getDefaultSource() {
    return defaultSource;
  }

  public void setDefaultSource(String defaultSource) {
    this.defaultSource = defaultSource;
  }

  public Duration getCacheTtl() {
    return cacheTtl;
  }

  public void setCacheTtl(Duration cacheTtl) {
    this.cacheTtl = cacheTtl;
  }
}
