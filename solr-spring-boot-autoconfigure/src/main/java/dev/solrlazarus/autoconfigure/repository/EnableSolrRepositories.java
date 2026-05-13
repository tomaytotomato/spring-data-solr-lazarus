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

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SolrRepositoriesRegistrar.class)
public @interface EnableSolrRepositories {
  String[] value() default {};
  String[] basePackages() default {};
  Class<?>[] basePackageClasses() default {};
  Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;
  ComponentScan.Filter[] includeFilters() default {};
  ComponentScan.Filter[] excludeFilters() default {};
  BootstrapMode bootstrapMode() default BootstrapMode.DEFAULT;
  String solrTemplateRef() default "solrTemplate";
}
