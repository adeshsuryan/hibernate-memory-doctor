package io.github.adeshsuryan.hibernatememorydoctor.autoconfigure;

import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctor;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Spring Boot auto-configuration: wraps {@link DataSource} beans in the
 * capture proxy, registers the persistence-context probe on the
 * SessionFactory, exposes a configured {@link MemoryDoctor}, and registers
 * the request-scope filter on servlet apps. Everything disables cleanly
 * via {@code hibernate-memory-doctor.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, MemoryDoctor.class})
@EnableConfigurationProperties(HibernateMemoryDoctorProperties.class)
public class HibernateMemoryDoctorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MemoryDoctor memoryDoctor(HibernateMemoryDoctorProperties props,
                              ObjectProvider<EntityManagerFactory> emfProvider,
                              Environment env) {
        MemoryDoctorConfig config = MemoryDoctorConfig.builder()
                .contextGrowthThreshold(props.getContextGrowthThreshold())
                .bulkLoadThreshold(props.getBulkLoadThreshold())
                .totalRowsBudget(props.getTotalRowsBudget())
                .wideRowThreshold(props.getWideRowThreshold())
                .entityLoadBudget(props.getEntityLoadBudget())
                .collectionLoadBudget(props.getCollectionLoadBudget())
                .collectionSizeThreshold(props.getCollectionSizeThreshold())
                .rowExplosionFactor(props.getRowExplosionFactor())
                .contextSpanNanos(props.getContextSpanMs() * 1_000_000L)
                .build();
        MemoryDoctor doctor = MemoryDoctor.withConfig(config, new HibernateStatsBridge(emfProvider));
        MemoryDoctor.setShared(doctor);
        return doctor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "hibernate-memory-doctor", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static DoctorDataSourceBeanPostProcessor doctorDataSourceBeanPostProcessor() {
        return new DoctorDataSourceBeanPostProcessor();
    }

    @Bean
    @ConditionalOnClass(name = "org.hibernate.event.service.spi.EventListenerRegistry")
    @ConditionalOnProperty(prefix = "hibernate-memory-doctor", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    PersistenceContextProbe persistenceContextProbe(
            ObjectProvider<EntityManagerFactory> emfProvider,
            HibernateMemoryDoctorProperties props) {
        return new PersistenceContextProbe(emfProvider, props.isEnabled() && props.isProbe());
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "hibernate-memory-doctor", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<MemoryDoctorRequestFilter> memoryDoctorRequestFilter(
            MemoryDoctor doctor, HibernateMemoryDoctorProperties props) {
        FilterRegistrationBean<MemoryDoctorRequestFilter> registration =
                new FilterRegistrationBean<>(
                        new MemoryDoctorRequestFilter(doctor, props.getReportMode()));
        registration.setName("hibernateMemoryDoctorRequestFilter");
        registration.setOrder(Integer.MAX_VALUE - 100);
        return registration;
    }
}
