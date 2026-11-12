package io.github.adeshsuryan.hibernatememorydoctor.autoconfigure;

import io.github.adeshsuryan.hibernatememorydoctor.MemoryStatsDelta;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Reads Hibernate {@link Statistics} snapshots around operation boundaries.
 * Resolves the {@link EntityManagerFactory} lazily so the MemoryDoctor bean
 * can be created before JPA initialization. Silent when JPA is absent.
 */
final class HibernateStatsBridge implements Supplier<MemoryStatsDelta> {

    private final ObjectProvider<EntityManagerFactory> emfProvider;

    HibernateStatsBridge(ObjectProvider<EntityManagerFactory> emfProvider) {
        this.emfProvider = emfProvider;
    }

    @Override
    public MemoryStatsDelta get() {
        EntityManagerFactory emf = emfProvider.getIfAvailable();
        if (emf == null) {
            return null;
        }
        try {
            SessionFactory sf = emf.unwrap(SessionFactory.class);
            Statistics stats = sf.getStatistics();
            if (!stats.isStatisticsEnabled()) {
                stats.setStatisticsEnabled(true);
            }
            return new MemoryStatsDelta(
                    stats.getEntityLoadCount(),
                    stats.getEntityFetchCount(),
                    stats.getCollectionLoadCount(),
                    stats.getCollectionFetchCount(),
                    stats.getFlushCount(),
                    stats.getQueryExecutionCount());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
