package com.veggofresh.notification.repository;

import com.veggofresh.notification.entity.Notification;
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
import java.util.Optional;
import java.util.UUID;

@Repository
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
}