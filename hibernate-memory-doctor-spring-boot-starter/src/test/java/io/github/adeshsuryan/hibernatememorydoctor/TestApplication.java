package io.github.adeshsuryan.hibernatememorydoctor;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
class TestApplication {
}

@Entity
@Table(name = "item")
class Item {

    @Id
    private Integer id;
    private String name;

    Item() {
    }

    Item(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}

@Entity
@Table(name = "doc_order")
class DocOrder {

    @Id
    private Integer id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order")
    private List<OrderLine> lines = new ArrayList<>();

    DocOrder() {
    }

    DocOrder(Integer id) {
        this.id = id;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}

@Entity
@Table(name = "order_line")
class OrderLine {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private DocOrder order;

    OrderLine() {
    }

    OrderLine(Integer id, DocOrder order) {
        this.id = id;
        this.order = order;
    }
}

@Repository
interface ItemRepository extends JpaRepository<Item, Integer> {
}

@Repository
interface OrderRepository extends JpaRepository<DocOrder, Integer> {
}

@Repository
interface LineRepository extends JpaRepository<OrderLine, Integer> {
}

@Service
class ImportService {

    private final ItemRepository items;

    private final OrderRepository orders;

    @PersistenceContext
    private EntityManager em;

    ImportService(ItemRepository items, OrderRepository orders) {
        this.items = items;
        this.orders = orders;
    }

    /** Deliberate bulk grow: hydrate every row into one context, never clear. */
    @Transactional(readOnly = true)
    public int bulkLoadAll() {
        return items.findAll().size();
    }

    /** Deliberate batch persist without clear - the OOM signature. */
    @Transactional
    public void undisciplinedPersist(int from, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new Item(from + i, "batch-" + i));
        }
        em.flush();
    }

    /** Correct pattern: flush + clear every 25 - the release valve. */
    @Transactional
    public void disciplinedPersist(int from, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new Item(from + i, "disc-" + i));
            if (i > 0 && i % 25 == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
    }

    /** Fat collection: touch the lazy 120-line collection inside a session. */
    @Transactional(readOnly = true)
    public int touchLines(int orderId) {
        return orders.findById(orderId).orElseThrow().getLines().size();
    }

    /** Clean path: bounded query. */
    @Transactional(readOnly = true)
    public long count() {
        return items.count();
    }
}
