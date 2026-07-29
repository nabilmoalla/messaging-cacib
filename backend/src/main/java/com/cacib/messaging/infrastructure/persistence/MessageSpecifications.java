package com.cacib.messaging.infrastructure.persistence;

import com.cacib.messaging.domain.model.MessageFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

final class MessageSpecifications {

    private MessageSpecifications() {
    }

    static Specification<MessageEntity> filter(MessageFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), MessageStatusEntity.valueOf(filter.status().name())));
            }
            if (filter.sourceQueue() != null) {
                predicates.add(cb.equal(root.get("sourceQueue"), filter.sourceQueue()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedAt"), filter.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
