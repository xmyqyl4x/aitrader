package com.myqyl.aitradex.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data")
public class MarketDataProperties {

  private String defaultSource = "alphavantage";
  private Duration cacheTtl = Duration.ofSeconds(30);
  private Duration streamPollInterval = Duration.ofSeconds(10);
  private Duration streamMaxDuration = Duration.ofMinutes(5);
  private Map<String, ProviderConfig> providers = new HashMap<>();

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

  public Duration getStreamPollInterval() {
    return streamPollInterval;
  }

  public void setStreamPollInterval(Duration streamPollInterval) {
    this.streamPollInterval = streamPollInterval;
  }

  public Duration getStreamMaxDuration() {
    return streamMaxDuration;
  }

  public void setStreamMaxDuration(Duration streamMaxDuration) {
    this.streamMaxDuration = streamMaxDuration;
  }

  public Map<String, ProviderConfig> getProviders() {
    return providers;
  }

  public void setProviders(Map<String, ProviderConfig> providers) {
    this.providers = providers;
  }

  /**
   * Configuration for individual market data providers.
   */
  public static class ProviderConfig {
    private boolean enabled = true;
    private int priority = 0;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getPriority() {
      return priority;
    }

    public void setPriority(int priority) {
      this.priority = priority;
    }
  }
}
