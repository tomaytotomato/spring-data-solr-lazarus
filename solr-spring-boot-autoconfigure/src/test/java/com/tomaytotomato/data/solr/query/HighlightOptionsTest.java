package com.tomaytotomato.data.solr.query;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HighlightOptionsTest {

  @Nested
  class Defaults {

    @Test
    void defaultPreTagIsEmTag() {
      assertThat(new HighlightOptions().getPreTag()).isEqualTo("<em>");
    }

    @Test
    void defaultPostTagIsEmCloseTag() {
      assertThat(new HighlightOptions().getPostTag()).isEqualTo("</em>");
    }

    @Test
    void defaultSnippetsIsOne() {
      assertThat(new HighlightOptions().getSnippets()).isEqualTo(1);
    }

    @Test
    void defaultFragsizeIsOneHundred() {
      assertThat(new HighlightOptions().getFragsize()).isEqualTo(100);
    }

    @Test
    void defaultFieldsListIsEmpty() {
      assertThat(new HighlightOptions().getFields()).isEmpty();
    }
  }

  @Nested
  class FluentSetters {

    @Test
    void preTagReturnsThisForChaining() {
      var options = new HighlightOptions();
      assertThat(options.preTag("<strong>")).isSameAs(options);
    }

    @Test
    void postTagReturnsThisForChaining() {
      var options = new HighlightOptions();
      assertThat(options.postTag("</strong>")).isSameAs(options);
    }

    @Test
    void snippetsReturnsThisForChaining() {
      var options = new HighlightOptions();
      assertThat(options.snippets(3)).isSameAs(options);
    }

    @Test
    void fragsizeReturnsThisForChaining() {
      var options = new HighlightOptions();
      assertThat(options.fragsize(200)).isSameAs(options);
    }

    @Test
    void preTagUpdatesValue() {
      var options = new HighlightOptions().preTag("<b>");
      assertThat(options.getPreTag()).isEqualTo("<b>");
    }

    @Test
    void postTagUpdatesValue() {
      var options = new HighlightOptions().postTag("</b>");
      assertThat(options.getPostTag()).isEqualTo("</b>");
    }

    @Test
    void snippetsUpdatesValue() {
      var options = new HighlightOptions().snippets(5);
      assertThat(options.getSnippets()).isEqualTo(5);
    }

    @Test
    void fragsizeUpdatesValue() {
      var options = new HighlightOptions().fragsize(250);
      assertThat(options.getFragsize()).isEqualTo(250);
    }
  }

  @Nested
  class AddField {

    @Test
    void addFieldReturnsThisForChaining() {
      var options = new HighlightOptions();
      assertThat(options.addField("title")).isSameAs(options);
    }

    @Test
    void addsSingleField() {
      var options = new HighlightOptions().addField("title");
      assertThat(options.getFields()).containsExactly("title");
    }

    @Test
    void addsMultipleFields() {
      var options = new HighlightOptions().addField("title").addField("description");
      assertThat(options.getFields()).containsExactly("title", "description");
    }
  }
}
