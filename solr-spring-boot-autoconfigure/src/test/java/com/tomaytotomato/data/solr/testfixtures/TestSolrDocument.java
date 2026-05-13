package com.tomaytotomato.data.solr.testfixtures;

import com.tomaytotomato.data.solr.mapping.SolrDocument;
import org.apache.solr.client.solrj.beans.Field;

@SolrDocument(collection = "test")
public class TestSolrDocument {

  @Field("id")
  public String id;
}
