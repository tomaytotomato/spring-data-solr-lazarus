package com.tomaytotomato.data.solr.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface SolrRepository<T, ID> extends PagingAndSortingRepository<T, ID>, CrudRepository<T, ID> {
}
