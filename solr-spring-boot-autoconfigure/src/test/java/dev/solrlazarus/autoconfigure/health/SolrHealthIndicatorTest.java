package dev.solrlazarus.autoconfigure.health;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolrHealthIndicatorTest {

  @Mock
  private SolrClient solrClient;

  @Mock
  private SolrPingResponse pingResponse;

  private SolrHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    healthIndicator = new SolrHealthIndicator(solrClient);
  }

  @Nested
  class WhenPingSucceeds {

    @Test
    void reportsUpWhenStatusIsZero() throws Exception {
      when(pingResponse.getStatus()).thenReturn(0);
      when(pingResponse.getElapsedTime()).thenReturn(42L);
      when(solrClient.ping()).thenReturn(pingResponse);

      var health = healthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void includesStatusDetailWhenUp() throws Exception {
      when(pingResponse.getStatus()).thenReturn(0);
      when(pingResponse.getElapsedTime()).thenReturn(42L);
      when(solrClient.ping()).thenReturn(pingResponse);

      var health = healthIndicator.health();

      assertThat(health.getDetails()).containsEntry("status", 0);
    }

    @Test
    void includesElapsedTimeDetailWhenUp() throws Exception {
      when(pingResponse.getStatus()).thenReturn(0);
      when(pingResponse.getElapsedTime()).thenReturn(42L);
      when(solrClient.ping()).thenReturn(pingResponse);

      var health = healthIndicator.health();

      assertThat(health.getDetails()).containsEntry("elapsed_time_ms", 42L);
    }

    @Test
    void reportsDownWhenStatusIsNonZero() throws Exception {
      when(pingResponse.getStatus()).thenReturn(500);
      when(solrClient.ping()).thenReturn(pingResponse);

      var health = healthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void includesStatusDetailWhenDown() throws Exception {
      when(pingResponse.getStatus()).thenReturn(500);
      when(solrClient.ping()).thenReturn(pingResponse);

      var health = healthIndicator.health();

      assertThat(health.getDetails()).containsEntry("status", 500);
    }
  }

  @Nested
  class WhenPingThrows {

    @Test
    void reportsDownWhenIOExceptionIsThrown() throws Exception {
      when(solrClient.ping()).thenThrow(new IOException("connection refused"));

      var health = healthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void reportsDownWhenSolrServerExceptionIsThrown() throws Exception {
      when(solrClient.ping()).thenThrow(new SolrServerException("ping failed"));

      var health = healthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void includesErrorMessageInDetailsWhenExceptionIsThrown() throws Exception {
      when(solrClient.ping()).thenThrow(new IOException("connection refused"));

      var health = healthIndicator.health();

      assertThat(health.getDetails()).containsKey("error");
    }
  }
}
