package io.github.adeshsuryan.hibernatememorydoctor.autoconfigure;

import io.github.adeshsuryan.hibernatememorydoctor.Capture;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Registers Hibernate event listeners that sample persistence-context
 * state at hydration time: POST_LOAD reports the first-level-cache size,
 * INIT_COLLECTION reports materialized collection sizes, and
 * CLEAR/EVICT report the release events batch loops rely on.
 *
 * Listeners sample only while a boundary is open on the calling thread —
 * outside a boundary the cost is one ThreadLocal read per event.
 */
final class PersistenceContextProbe implements SmartInitializingSingleton {

    private final ObjectProvider<EntityManagerFactory> emfProvider;
    private final boolean enabled;

    PersistenceContextProbe(ObjectProvider<EntityManagerFactory> emfProvider,
                            boolean enabled) {
        this.emfProvider = emfProvider;
        this.enabled = enabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!enabled) {
            return;
        }
        EntityManagerFactory emf = emfProvider.getIfAvailable();
        if (emf == null) {
            return;
        }
        EventListenerRegistry registry = registry(emf);
        if (registry == null) {
            return;
        }
        registry.appendListeners(EventType.POST_LOAD, (PostLoadEventListener) event -> {
            if (Capture.isActive()) {
                Capture.recordEntityLoad(event.getSession(), managedEntities(event));
            }
        });
        registry.appendListeners(EventType.INIT_COLLECTION,
                (InitializeCollectionEventListener) event -> {
                    if (Capture.isActive()) {
                        Capture.recordCollectionLoad(event.getSession(), collectionSize(event));
                    }
                });
        registry.appendListeners(EventType.CLEAR, event -> {
            if (Capture.isActive()) {
                Capture.recordClear(event.getSession());
            }
        });
        registry.appendListeners(EventType.EVICT, event -> {
            if (Capture.isActive()) {
                Capture.recordClear(event.getSession());
            }
        });
    }

    private static EventListenerRegistry registry(EntityManagerFactory emf) {
        try {
            return emf.unwrap(SessionFactoryImplementor.class)
                    .getServiceRegistry()
                    .getService(EventListenerRegistry.class);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static int managedEntities(org.hibernate.event.spi.PostLoadEvent event) {
        try {
            return event.getSession().getPersistenceContext().getNumberOfManagedEntities();
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }

    private static int collectionSize(org.hibernate.event.spi.InitializeCollectionEvent event) {
        try {
            return event.getCollection().getSize();
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }
}
