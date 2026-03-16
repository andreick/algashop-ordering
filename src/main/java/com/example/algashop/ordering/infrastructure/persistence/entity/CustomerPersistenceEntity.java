package com.example.algashop.ordering.infrastructure.persistence.entity;

import com.example.algashop.ordering.infrastructure.persistence.embeddable.AddressEmbeddable;
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
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Table(name = "\"customer\"")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CustomerPersistenceEntity {

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
}
