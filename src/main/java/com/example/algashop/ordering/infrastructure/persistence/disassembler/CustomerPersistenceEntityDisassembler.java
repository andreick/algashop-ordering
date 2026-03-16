package com.example.algashop.ordering.infrastructure.persistence.disassembler;

import com.example.algashop.ordering.domain.model.entity.Customer;
import com.example.algashop.ordering.domain.model.valueobject.Address;
import com.example.algashop.ordering.domain.model.valueobject.BirthDate;
import com.example.algashop.ordering.domain.model.valueobject.Document;
import com.example.algashop.ordering.domain.model.valueobject.Email;
import com.example.algashop.ordering.domain.model.valueobject.FullName;
import com.example.algashop.ordering.domain.model.valueobject.LoyaltyPoints;
import com.example.algashop.ordering.domain.model.valueobject.Phone;
import com.example.algashop.ordering.domain.model.valueobject.ZipCode;
import com.example.algashop.ordering.domain.model.valueobject.id.CustomerId;
import com.example.algashop.ordering.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.example.algashop.ordering.infrastructure.persistence.entity.CustomerPersistenceEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerPersistenceEntityDisassembler {

    public Customer toDomainEntity(CustomerPersistenceEntity entity) {
        return Customer.existing()
                .id(new CustomerId(entity.getId()))
                .fullName(new FullName(entity.getFirstName(), entity.getLastName()))
                .birthDate(entity.getBirthDate() != null ? new BirthDate(entity.getBirthDate()) : null)
                .email(new Email(entity.getEmail()))
                .phone(new Phone(entity.getPhone()))
                .document(new Document(entity.getDocument()))
                .loyaltyPoints(new LoyaltyPoints(entity.getLoyaltyPoints()))
                .promotionNotificationsAllowed(entity.getPromotionNotificationsAllowed())
                .archived(entity.getArchived())
                .registeredAt(entity.getRegisteredAt())
                .archivedAt(entity.getArchivedAt())
                .address(toValueObject(entity.getAddress()))
                .build();
    }

    private Address toValueObject(AddressEmbeddable address) {
        return Address.builder()
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .neighborhood(address.getNeighborhood())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(new ZipCode(address.getZipCode()))
                .build();
    }
}
