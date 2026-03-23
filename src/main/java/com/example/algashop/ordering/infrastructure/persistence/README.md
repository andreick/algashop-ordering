# Infraestrutura de Persistência

A camada de infraestrutura implementa a persistência dos agregados de domínio em banco de dados relacional, mantendo a independência entre o modelo de domínio e a tecnologia de armazenamento.

## Estrutura

```
infrastructure/
├── persistence/
│   ├── entity/                # Entidades JPA
│   ├── embeddable/            # Propriedades compostas mapeadas
│   ├── repository/            # Spring Data JPA
│   ├── provider/              # Implementações dos contratos de domínio
│   ├── assembler/             # Domain → Persistence
│   ├── disassembler/          # Persistence → Domain
│   └── config/                # Configuração (auditing, etc)
```

## Fluxo de Persistência

```
Domain Aggregate (Customer/Order)
    ↓
Provider (CustomersProvider / OrdersProvider)
    ↓ assembler.fromDomain()
Persistence Entity
    ↓
Spring Data JPA Repository
    ↓ saveAndFlush()
Database
```

Carregamento (reverso):

```
Database
    ↓
Spring Data JPA Repository
    ↓ findById()
Persistence Entity
    ↓ disassembler.toDomainEntity()
Domain Aggregate
```

## Entidades JPA

### CustomerPersistenceEntity

Mapeamento de `Customer` do domínio:

- `@Id private UUID id` → CustomerId
- Campos decompostos: `firstName`, `lastName` → FullName
- `@Version private Long version` → Controle otimista de concorrência
- `@Embedded private AddressEmbeddable address` → Propriedade composta

Anotadores de auditoria:
- `@CreatedBy` → Usuário que criou
- `@LastModifiedDate` → Último timestamp
- `@LastModifiedBy` → Usuário que modificou

### OrderPersistenceEntity

Mapeamento de `Order` do domínio:

- `@Id private Long id` → OrderId (TSID → Long)
- `@ManyToOne private CustomerPersistenceEntity customer` → Relacionamento com cliente
- `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` → Itens do pedido
- `@Embedded` para propriedades compostas: Billing, Shipping
- `@Version private Long version` → Otimistic locking

Estados armazenados como String:
- `status` → Enum `OrderStatus` (DRAFT, PLACED, PAID, READY, CANCELED)
- `paymentMethod` → Enum `PaymentMethod`

### OrderItemPersistenceEntity

Entidade filha dentro do agregado Order:

- `@Id private Long id` → OrderItemId (TSID → Long)
- `@ManyToOne private OrderPersistenceEntity order` → Referência bidireccional
- Campos desnormalizados: `productId`, `productName`, `price`, `quantity`

Benefício: Reconstitui histórico de pedido sem depender de estado externo de Product.

## Embeddables

Value Objects do domínio mapeados como columns agrupadas com `@Embeddable` e `@AttributeOverride`:

### AddressEmbeddable

```java
@Embeddable
public class AddressEmbeddable {
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String zipCode;
}
```

Uso em entidade:

```java
@Embedded
@AttributeOverride(name = "street", column = @Column(name = "address_street"))
@AttributeOverride(name = "number", column = @Column(name = "address_number"))
// ... resto dos atributos mapeados
private AddressEmbeddable address;
```

### BillingEmbeddable e ShippingEmbeddable

Compostos incluem `@Embedded` aninhadas:

```java
@Embeddable
public class BillingEmbeddable {
    private String firstName;
    private String lastName;
    private String document;
    private String phone;
    private String email;
    @Embedded
    private AddressEmbeddable address;  // Nested
}
```

Em `OrderPersistenceEntity`, cada um mapeia em prefixo de coluna:

```java
@Embedded
@AttributeOverride(name = "firstName", column = @Column(name = "billing_first_name"))
@AttributeOverride(name = "address.street", column = @Column(name = "billing_address_street"))
// ... completo para todos os atributos
private BillingEmbeddable billing;

@Embedded
@AttributeOverride(name = "cost", column = @Column(name = "shipping_cost"))
@AttributeOverride(name = "recipient.firstName", column = @Column(name = "shipping_recipient_first_name"))
// ... completo
private ShippingEmbeddable shipping;
```

**Benefício:** Mantém tabelas desnormalizadas e permitem buscar "toda informação de um pedido" sem joins.

## Repository Pattern

Na infraestrutura, o papel é implementar o contrato de domínio usando Spring Data JPA.

Separação aplicada:

1. **Contrato de domínio (DDD)** em `domain/model/repository/*`
2. **Adaptador de persistência** em `persistence/provider/*`
3. **Tecnologia de acesso a dados** em `persistence/repository/*` (Spring Data)

### 1) Contrato de domínio

As interfaces de domínio descrevem o que o caso de uso precisa, sem dependência de JPA:

```java
public interface Customers extends Repository<Customer, CustomerId> {
    Optional<Customer> ofEmail(Email email);
    boolean isEmailUnique(Email email, CustomerId exceptCustomerId);
}
```

```java
public interface Orders extends Repository<Order, OrderId> {
    List<Order> placedByCustomerInYear(CustomerId customerId, Year year);
    long salesQuantityByCustomerInYear(CustomerId customerId, Year year);
    Money totalSoldForCustomer(CustomerId customerId);
}
```

### 2) Implementação do contrato (Providers)

Os providers implementam os contratos do domínio e fazem a ponte para a infraestrutura usando assembler/disassembler.

```java
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomersPersistenceProvider implements Customers {

    @Override
    public Optional<Customer> ofId(CustomerId customerId) {
        return persistenceRepository.findById(customerId.value())
                .map(disassembler::toDomainEntity);
    }

    @Override
    @Transactional
    public void add(Customer aggregateRoot) {
        persistenceRepository.findById(aggregateRoot.id().value())
                .ifPresentOrElse(
                        entity -> assembler.merge(entity, aggregateRoot),
                        () -> persistenceRepository.saveAndFlush(assembler.fromDomain(aggregateRoot)));
    }
}
```

```java
@Override
@Transactional
public void add(Order aggregateRoot) {
    persistenceRepository.findById(aggregateRoot.id().value().toLong())
            .ifPresentOrElse(
                    entity -> {
                        assembler.merge(entity, aggregateRoot);
                        persistenceRepository.flush();
                        versionSynchronizer.synchronizeVersion(aggregateRoot, entity.getVersion());
                    },
                    () -> {
                        OrderPersistenceEntity entity = assembler.fromDomain(aggregateRoot);
                        persistenceRepository.saveAndFlush(entity);
                        versionSynchronizer.synchronizeVersion(aggregateRoot, entity.getVersion());
                    });
}
```

### 3) Repositórios Spring Data JPA

As interfaces JPA são **detalhe de infraestrutura**, não o Repository Pattern de domínio.
Elas encapsulam queries e projeções de leitura usadas pelos providers:

```java
public interface CustomerPersistenceEntityRepository extends JpaRepository<CustomerPersistenceEntity, UUID> {
    Optional<CustomerPersistenceEntity> findByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID customerId);
}
```

```java
@Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM OrderPersistenceEntity o
        WHERE o.customer.id = :customerId
        AND o.paidAt IS NOT NULL
        AND o.canceledAt IS NULL
        """)
BigDecimal totalSoldForCustomer(@Param("customerId") UUID customerId);
```

### Resultado prático do padrão no projeto

- O Repository Pattern fica no domínio (interfaces `Customers` e `Orders`).
- O domínio depende de **interfaces**, não de Spring Data.
- A infraestrutura concentra detalhes técnicos (JPA, `@Query`, `flush`, lock otimista).
- `assembler`/`disassembler` evitam vazamento de anotações JPA para o domínio.
- Regras de escrita (`add`) e leitura (`ofId`, queries analíticas) ficam separadas e claras.

## Assembler: Domain → Persistence

Converte agregado de domínio para entidade JPA antes de persistir.

### CustomerPersistenceEntityAssembler

```java
@Component
public class CustomerPersistenceEntityAssembler {

    public CustomerPersistenceEntity merge(CustomerPersistenceEntity entity, Customer customer) {
        entity.setId(customer.id().value());
        entity.setFirstName(customer.fullName().firstName());
        entity.setEmail(customer.email().value());
        entity.setAddress(toEmbeddable(customer.address()));
        return entity;
    }

    // ...demais mapeamentos e toEmbeddable(Address)
}
```

**Fluxo:**
- Extrai valores primitivos de Value Objects
- Decompõe Address → AddressEmbeddable
- `merge()` permite update sem criar nova entidade (economiza version bump desnecessário)

### OrderPersistenceEntityAssembler

```java
@Component
@RequiredArgsConstructor
public class OrderPersistenceEntityAssembler {

    private final CustomerPersistenceEntityRepository customerPersistenceEntityRepository;

    public OrderPersistenceEntity merge(OrderPersistenceEntity entity, Order order) {
        entity.setId(order.id().value().toLong());
        entity.setCustomer(customerPersistenceEntityRepository
            .getReferenceById(order.customerId().value()));  // Lazy - sem SELECT
        entity.setStatus(order.status().name());
        entity.setBilling(toBillingEmbeddable(order.billing()));
        mergeItems(order, entity);  // Sincroniza items
        return entity;
    }

    private void mergeItems(Order order, OrderPersistenceEntity orderEntity) {
        // Limpa estado atual e reaplica itens do agregado
        // (detalhes de merge omitidos para foco didático)
    }

    // ...toBillingEmbeddable(), toShippingEmbeddable(), toAddressEmbeddable()
}
```

**Pontos chave:**
- `getReferenceById()` evita SELECT para relacionamento (lazy)
- `mergeItems()` sincroniza itens de forma eficiente
- Decompõe `Shipping` / `Billing` / `Address` em embeddables

## Disassembler: Persistence → Domain

Reconstitui aggregado completo a partir de entidade JPA, sem deixar dependências de framework.

### CustomerPersistenceEntityDisassembler

```java
@Component
public class CustomerPersistenceEntityDisassembler {

    public Customer toDomainEntity(CustomerPersistenceEntity entity) {
        return Customer.existing()
                .id(new CustomerId(entity.getId()))
                .fullName(new FullName(entity.getFirstName(), entity.getLastName()))
                .email(new Email(entity.getEmail()))
                .address(toValueObject(entity.getAddress()))
                .build();
    }

    // ...toValueObject(AddressEmbeddable) e demais campos
}
```

**Reconstrução:** Wrapper primitivos novamente em Value Objects (Email, Phone, CustomerId, etc).

### OrderPersistenceEntityDisassembler

```java
@Component
public class OrderPersistenceEntityDisassembler {

    public Order toDomainEntity(OrderPersistenceEntity entity) {
        return Order.existing()
                .id(new OrderId(entity.getId()))
                .customerId(new CustomerId(entity.getCustomerId()))
                .totalAmount(new Money(entity.getTotalAmount()))
                .billing(toValueObject(entity.getBilling()))
                .status(OrderStatus.valueOf(entity.getStatus()))
                .items(toDomainEntity(entity.getItems()))
                .build();  // ← Reconstitui completo
    }

    private Set<OrderItem> toDomainEntity(Set<OrderItemPersistenceEntity> items) {
        return items.stream()
                .map(item -> OrderItem.existing()
                        .id(new OrderItemId(item.getId()))
                        .quantity(new Quantity(item.getQuantity()))
                        .build())
                .collect(Collectors.toSet());
    }

    // ...toValueObject(Shipping/Billing/Address)
}
```

**Fluxo inverso:** Reconstitui ordem completa, incluindo items, reenvolve valores primitivos em VOs corretos.

## Optimistic Locking

Ambas `CustomerPersistenceEntity` e `OrderPersistenceEntity` usam `@Version` para controle otimista:

```java
@Version
private Long version;
```

Quando uma entidade é modificada e flushed, JPA incrementa `version` automaticamente. Se dois updates concorrentes tentam persistir a mesma versão, o segundo lança `OptimisticLockException`.

### VersionSynchronizer

Para `Order`, após persist, a versão no banco é sincronizada de volta ao domínio:

```java
public interface VersionSynchronizer<T> {
    void synchronizeVersion(T entity, Long version);
}

@Component
public class OrderVersionSynchronizer implements VersionSynchronizer<Order> {
    @Override
    public void synchronizeVersion(Order order, Long version) {
        order.setVersion(version);  // Package-private permite isto
    }
}
```

**Razão:** Em transações subsequentes, Order retém versão para próximas atualizações.

## Auditoria

Configurada em `config/SpringDataAuditingConfig.java`:

```java
@Configuration
@EnableJpaAuditing(
    dateTimeProviderRef = "auditingDateTimeProvider", 
    auditorAwareRef = "auditorProvider")
public class SpringDataAuditingConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Bean
    AuditorAware<UUID> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID());
    }
}
```

Anotadores em entidades:
- `@CreatedBy` → Quem criou
- `@LastModifiedDate` → Quando modificado
- `@LastModifiedBy` → Quem modificou

Estes campos são preenchidos automaticamente antes de cada flush.

## Padrão: Persistência em Cascata

Items do Order têm `cascade = CascadeType.ALL` + `orphanRemoval = true`:

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<OrderItemPersistenceEntity> items = new HashSet<>();
```

Benefício:
- Salvar Order persiste items automaticamente
- Remover item do set deleta no banco (orphan removal)
- Agregado permanece coeso em uma transação

## Resumo de Responsabilidades

| Componente | Responsabilidade |
|-----------|------------------|
| **Provider** | Implementar contrato de domínio, orquestrar assembler/disassembler |
| **Assembler** | Domain → Persistence (desnormalização de VOs, decompor embeddables) |
| **Disassembler** | Persistence → Domain (normalização, reenvolver primitivos em VOs) |
| **JPA Repository** | Queries JPQL, primitivas de pesquisa |
| **Embeddable** | Agrupar columns sob conceito unificado |
| **VersionSynchronizer** | Manter versão em sincronismo para optimistic lock |
