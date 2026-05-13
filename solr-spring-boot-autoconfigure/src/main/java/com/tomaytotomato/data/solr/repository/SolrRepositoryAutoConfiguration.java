package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.SolrAutoConfiguration;
import com.tomaytotomato.data.solr.SolrTemplate;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = SolrAutoConfiguration.class)
@ConditionalOnClass({SolrClient.class, SolrRepository.class})
@ConditionalOnBean(SolrTemplate.class)
@ConditionalOnMissingBean(SolrRepositoryFactoryBean.class)
@Import(SolrRepositoriesRegistrar.class)
public class SolrRepositoryAutoConfiguration {
}
