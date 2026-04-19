package com.example.algashop.ordering.infrastructure.persistence.shoppingcart;

import com.example.algashop.ordering.infrastructure.persistence.customer.CustomerPersistenceEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "\"shopping_cart\"")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShoppingCartPersistenceEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;
    private BigDecimal totalAmount;
    private Integer totalItems;

    @JoinColumn
    @ManyToOne(optional = false)
    private CustomerPersistenceEntity customer;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL)
    private Set<ShoppingCartItemPersistenceEntity> items = new HashSet<>();

    @CreatedBy
    private UUID createdByUserId;

    @CreatedDate
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime lastModifiedAt;

    @LastModifiedBy
    private UUID lastModifiedByUserId;

    @Version
    private Long version;

    @Builder(toBuilder = true)
    public ShoppingCartPersistenceEntity(UUID id, CustomerPersistenceEntity customer, BigDecimal totalAmount,
            Integer totalItems, OffsetDateTime createdAt,
            Set<ShoppingCartItemPersistenceEntity> items) {
        this.id = id;
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.totalItems = totalItems;
        this.createdAt = createdAt;
        this.replaceItems(items);
    }

    public void addItem(@NonNull Set<ShoppingCartItemPersistenceEntity> items) {
        for (ShoppingCartItemPersistenceEntity item : items) {
            addItem(item);
        }
    }

    public void addItem(@NonNull ShoppingCartItemPersistenceEntity item) {
        item.setShoppingCart(this);
        this.items.add(item);
    }

    public void replaceItems(@NonNull Set<ShoppingCartItemPersistenceEntity> updatedItems) {
        clearItems();
        updatedItems.forEach(this::addItem);
    }

    public void clearItems() {
        this.items.clear();
    }

    public UUID getCustomerId() {
        return this.customer != null ? this.customer.getId() : null;
    }
}
