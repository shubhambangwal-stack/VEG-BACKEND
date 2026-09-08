package com.veggofresh.notification.repository;

import com.veggofresh.notification.entity.Notification;
<<<<<<< HEAD
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
=======
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
import java.util.Optional;
import java.util.UUID;

@Repository
<<<<<<< HEAD
public class NotificationRepository extends SimpleJpaRepository<Notification, UUID> {

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationRepository(EntityManager entityManager) {
        super(Notification.class, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(entityManager.createQuery(
                        "SELECT n FROM Notification n WHERE n.id = :id AND n.deletedAt IS NULL",
                        Notification.class)
                .setParameter("id", id)
                .getSingleResult());
    }

    @Transactional(readOnly = true)
    public List<Notification> findAllByRecipient(String recipientType, UUID recipientId, Pageable pageable) {
        jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        jakarta.persistence.criteria.CriteriaQuery<Notification> query = cb.createQuery(Notification.class);
        jakarta.persistence.criteria.Root<Notification> root = query.from(Notification.class);

        jakarta.persistence.criteria.Predicate recipientFilter = cb.equal(root.get("recipientType"), recipientType);
        jakarta.persistence.criteria.Predicate idFilter = cb.equal(root.get("recipientId"), recipientId);

        query.where(recipientFilter, idFilter);
        query.orderBy(cb.desc(root.get("sentAt")));

        jakarta.persistence.TypedQuery<Notification> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(pageable.getPageSize());
        typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());

        return typedQuery.getResultList();
    }

    @Transactional(readOnly = true)
    public long countByRecipient(String recipientType, UUID recipientId) {
        jakarta.persistence.criteria.CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        jakarta.persistence.criteria.CriteriaQuery<Long> query = cb.createQuery(Long.class);
        jakarta.persistence.criteria.Root<Notification> root = query.from(Notification.class);

        jakarta.persistence.criteria.Predicate recipientFilter = cb.equal(root.get("recipientType"), recipientType);
        jakarta.persistence.criteria.Predicate idFilter = cb.equal(root.get("recipientId"), recipientId);

        query.where(recipientFilter, idFilter);

        return entityManager.createQuery(query).getSingleResult();
    }
=======
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Newest-first inbox for one recipient (the {@code @Where} filter drops soft-deleted rows). */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /** Badge count: how many unread notifications this recipient has. */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /** Ownership-scoped read so a user can never mutate another recipient's row. */
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    /** Ownership-scoped "all read" marker update — single statement, no row-by-row round-trips. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllRead(@Param("recipientId") UUID recipientId);
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
}