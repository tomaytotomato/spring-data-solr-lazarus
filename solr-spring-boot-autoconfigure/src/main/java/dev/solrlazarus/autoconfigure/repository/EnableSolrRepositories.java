package dev.solrlazarus.autoconfigure.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SolrRepositoriesRegistrar.class)
public @interface EnableSolrRepositories {
  String[] value() default {};
  String[] basePackages() default {};
  Class<?>[] basePackageClasses() default {};
  ComponentScan.Filter[] includeFilters() default {};
  ComponentScan.Filter[] excludeFilters() default {};
  Class<?> repositoryFactoryBeanClass() default SolrRepositoryFactoryBean.class;
  Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;
  String namedQueriesLocation() default "";
  QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;
  String repositoryImplementationPostfix() default "Impl";
  boolean considerNestedRepositories() default false;
  BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;
  String solrTemplateRef() default "solrTemplate";
}
