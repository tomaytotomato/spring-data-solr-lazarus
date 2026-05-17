package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.query.HighlightOptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HighlightAnnotationTest {

  @Nested
  class AnnotationDefaults {

    @Highlight
    void annotatedWithDefaults() {}

    @Test
    void defaultFieldsIsEmpty() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Highlight.class);

      assertThat(annotation.fields()).isEmpty();
    }

    @Test
    void defaultFragsizeIs100() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Highlight.class);

      assertThat(annotation.fragsize()).isEqualTo(100);
    }

    @Test
    void defaultSnippetsIs1() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Highlight.class);

      assertThat(annotation.snippets()).isEqualTo(1);
    }

    @Test
    void defaultPrefixIsEmTag() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Highlight.class);

      assertThat(annotation.prefix()).isEqualTo("<em>");
    }

    @Test
    void defaultPostfixIsEmCloseTag() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Highlight.class);

      assertThat(annotation.postfix()).isEqualTo("</em>");
    }
  }

  @Nested
  class CustomAttributes {

    @Highlight(fields = {"title", "description"}, fragsize = 200, snippets = 3,
        prefix = "<mark>", postfix = "</mark>")
    void annotatedWithCustomValues() {}

    @Test
    void customFieldsArePreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Highlight.class);

      assertThat(annotation.fields()).containsExactly("title", "description");
    }

    @Test
    void customFragsizeIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Highlight.class);

      assertThat(annotation.fragsize()).isEqualTo(200);
    }

    @Test
    void customSnippetsIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Highlight.class);

      assertThat(annotation.snippets()).isEqualTo(3);
    }

    @Test
    void customPrefixIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Highlight.class);

      assertThat(annotation.prefix()).isEqualTo("<mark>");
    }

    @Test
    void customPostfixIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Highlight.class);

      assertThat(annotation.postfix()).isEqualTo("</mark>");
    }
  }

  @Nested
  class OptionsConstruction {

    @Test
    void buildsHighlightOptionsFromAnnotationDefaults() throws Exception {
      var method = AnnotationDefaults.class.getDeclaredMethod("annotatedWithDefaults");
      var annotation = method.getAnnotation(Highlight.class);

      var options = HighlightAnnotationAdapter.toHighlightOptions(annotation);

      assertThat(options.getPreTag()).isEqualTo("<em>");
      assertThat(options.getPostTag()).isEqualTo("</em>");
      assertThat(options.getSnippets()).isEqualTo(1);
      assertThat(options.getFragsize()).isEqualTo(100);
      assertThat(options.getFields()).isEmpty();
    }

    @Test
    void buildsHighlightOptionsFromCustomAnnotation() throws Exception {
      var method = CustomAttributes.class.getDeclaredMethod("annotatedWithCustomValues");
      var annotation = method.getAnnotation(Highlight.class);

      var options = HighlightAnnotationAdapter.toHighlightOptions(annotation);

      assertThat(options.getPreTag()).isEqualTo("<mark>");
      assertThat(options.getPostTag()).isEqualTo("</mark>");
      assertThat(options.getSnippets()).isEqualTo(3);
      assertThat(options.getFragsize()).isEqualTo(200);
      assertThat(options.getFields()).containsExactly("title", "description");
    }
  }
}
