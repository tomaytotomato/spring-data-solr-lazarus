package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.query.Criteria;
import com.tomaytotomato.data.solr.query.SimpleQuery;
import java.util.Collection;
import java.util.Iterator;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

public class SolrQueryCreator extends AbstractQueryCreator<SimpleQuery, Criteria> {

  private final SolrFieldNameResolver fieldNameResolver;

  public SolrQueryCreator(PartTree tree, ParameterAccessor parameters) {
    this(tree, parameters, SolrFieldNameResolver.identity());
  }

  public SolrQueryCreator(PartTree tree, ParameterAccessor parameters, SolrFieldNameResolver fieldNameResolver) {
    super(tree, parameters);
    this.fieldNameResolver = fieldNameResolver;
  }

  @Override
  protected Criteria create(Part part, Iterator<Object> iterator) {
    return from(part, iterator);
  }

  @Override
  protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {
    return base.and(from(part, iterator));
  }

  @Override
  protected Criteria or(Criteria base, Criteria criteria) {
    return base.or(criteria);
  }

  @Override
  protected SimpleQuery complete(Criteria criteria, Sort sort) {
    var query = new SimpleQuery(criteria);
    if (sort.isSorted()) {
      query.setSort(sort);
    }
    return query;
  }

  @SuppressWarnings("unchecked")
  private Criteria from(Part part, Iterator<Object> iterator) {
    var field = fieldNameResolver.resolve(part.getProperty().getSegment());
    return switch (part.getType()) {
      case SIMPLE_PROPERTY -> Criteria.where(field).is(iterator.next());
      case NEGATING_SIMPLE_PROPERTY -> Criteria.where(field).isNot(iterator.next());
      case CONTAINING -> Criteria.where(field).contains((String) iterator.next());
      case NOT_CONTAINING -> Criteria.where(field).notContains((String) iterator.next());
      case STARTING_WITH -> Criteria.where(field).startsWith((String) iterator.next());
      case ENDING_WITH -> Criteria.where(field).endsWith((String) iterator.next());
      case GREATER_THAN, AFTER -> Criteria.where(field).greaterThan(iterator.next());
      case GREATER_THAN_EQUAL -> Criteria.where(field).greaterThanEqual(iterator.next());
      case LESS_THAN, BEFORE -> Criteria.where(field).lessThan(iterator.next());
      case LESS_THAN_EQUAL -> Criteria.where(field).lessThanEqual(iterator.next());
      case BETWEEN -> Criteria.where(field).between(iterator.next(), iterator.next());
      case IN -> Criteria.where(field).in((Collection<?>) iterator.next());
      case IS_NULL -> Criteria.where(field).isNull();
      case IS_NOT_NULL -> Criteria.where(field).isNotNull();
      case TRUE -> Criteria.where(field).is(true);
      case FALSE -> Criteria.where(field).is(false);
      default -> throw new UnsupportedOperationException(
          "Unsupported query keyword: " + part.getType());
    };
  }
}
