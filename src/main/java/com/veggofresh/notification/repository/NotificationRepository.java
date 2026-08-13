package com.veggofresh.notification.repository;

import com.veggofresh.notification.entity.Notification;
import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NotificationRepository extends SimpleJpaRepository<Notification, UUID> {

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationRepository(javax.persistence.EntityManager entityManager,
                                  jakarta.transaction.EntityManager transactionEntityManager) {
        super(Notification.class, transactionEntityManager);
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

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findAllByRecipient(String recipientType, UUID recipientId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Notification> query = cb.createQuery(Notification.class);
        Root<Notification> root = query.from(Notification.class);

        Predicate recipientFilter = cb.equal(root.get("recipientType"), recipientType);
        Predicate idFilter = cb.equal(root.get("recipientId"), recipientId);

        query.where(recipientFilter, idFilter);
        query.orderBy(cb.desc(root.get("sentAt")));

        TypedQuery<Notification> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(pageable.getPageSize());
        typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());

        return typedQuery.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRecipient(String recipientType, UUID recipientId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Notification> root = query.from(Notification.class);

        Predicate recipientFilter = cb.equal(root.get("recipientType"), recipientType);
        Predicate idFilter = cb.equal(root.get("recipientId"), recipientId);

        query.where(recipientFilter, idFilter);

        return entityManager.createQuery(query).getSingleResult();
    }
}