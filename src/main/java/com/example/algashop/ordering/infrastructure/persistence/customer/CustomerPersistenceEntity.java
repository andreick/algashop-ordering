package com.example.algashop.ordering.infrastructure.persistence.customer;

import com.example.algashop.ordering.infrastructure.persistence.commons.AddressEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@Table(name = "\"customer\"")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CustomerPersistenceEntity extends AbstractAggregateRoot<CustomerPersistenceEntity> {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String document;
    private Boolean promotionNotificationsAllowed;
    private Boolean archived;
    private OffsetDateTime registeredAt;
    private OffsetDateTime archivedAt;
    @Embedded
    @AttributeOverride(name = "street", column = @Column(name = "address_street"))
    @AttributeOverride(name = "number", column = @Column(name = "address_number"))
    @AttributeOverride(name = "complement", column = @Column(name = "address_complement"))
    @AttributeOverride(name = "neighborhood", column = @Column(name = "address_neighborhood"))
    @AttributeOverride(name = "city", column = @Column(name = "address_city"))
    @AttributeOverride(name = "state", column = @Column(name = "address_state"))
    @AttributeOverride(name = "zipCode", column = @Column(name = "address_zipCode"))
    private AddressEmbeddable address;
    private Integer loyaltyPoints;

    @Version
    private Long version;

    @CreatedBy
    private UUID createdByUserId;

    @LastModifiedDate
    private OffsetDateTime lastModifiedAt;

    @LastModifiedBy
    private UUID lastModifiedByUserId;

    public Collection<Object> getEvents() {
        return super.domainEvents();
    }

    public void addEvents(@NonNull Collection<Object> events) {
        for (Object event : events) {
            this.registerEvent(event);
        }
    }
}
