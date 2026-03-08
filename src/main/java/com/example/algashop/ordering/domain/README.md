# Domain-Driven Design no Microsserviço de Ordering

Fundamentos de **Domain-Driven Design (DDD)** aplicados à implementação do domínio de um microsserviço.

## 📚 Índice

1. [Introdução ao Domain-Driven Design](#introducao)
2. [Entities (Domain Entities)](#entities)
3. [Rich Domain Model vs Anemic Domain Model](#rich-vs-anemic)
4. [Identificadores e UUIDs](#identificadores)
5. [Value Objects](#value-objects)
6. [Agregados e Aggregate Root](#agregados)
7. [Regras de Negócio e Comportamento](#regras-negocio)
8. [Validações e Encapsulamento](#validacoes)
9. [Exceções de Domínio](#excecoes)
10. [Princípios de Tamanho e Consistência](#invariantes)
11. [Factories e Builders](#factories)
12. [Transações e Coordenação entre Agregados](#transacoes)

## <a name="introducao"></a>1. Introdução ao Domain-Driven Design

**Domain-Driven Design** é uma metodologia proposta por Eric Evans que coloca o **domínio do negócio** no centro da arquitetura de software. Entidades não são apenas tabelas de banco ou estruturas de dados, mas **objetos que representam conceitos do negócio e encapsulam suas regras**.

### Pilares do DDD

- **Domain Entity**: Objeto com identidade única e ciclo de vida
- **Value Object**: Objeto sem identidade, definido por seus atributos
- **Aggregate**: Grupo de entidades e value objects tratados como unidade
- **Factory**: Padrão para criar objetos complexos
- **Repository**: Abstração para persistência
- **Domain Service**: Lógica que não pertence a nenhuma entidade específica
- **Domain Exception**: Exceções que representam violações de regras de negócio

## <a name="entities"></a>2. Entities (Domain Entities)

### O que é uma Domain Entity?

Uma **Domain Entity** é um objeto que:
- Possui **identidade única** que persiste ao longo do tempo
- Tem um **ciclo de vida** definido no domínio
- **Encapsula regras de negócio** relacionadas ao conceito que representa
- Possui **comportamentos** específicos do domínio (não apenas getters/setters)
- É responsável por manter a **integridade dos dados**

### Exemplo: Customer Entity

```java
@Accessors(fluent = true)
@Getter
public class Customer {
    private CustomerId id;                         // ← Identidade única
    private FullName fullName;
    private BirthDate birthDate;
    private Email email;
    private Phone phone;
    private Document document;
    private Boolean isPromotionNotificationsAllowed;
    private Boolean isArchived;
    private OffsetDateTime registeredAt;
    private OffsetDateTime archivedAt;
    private LoyaltyPoints loyaltyPoints;
    private Address address;

    // Comportamentos do domínio
    public void addLoyaltyPoints(LoyaltyPoints loyaltyPointsAdded) { ... }
    public void archive() { ... }
    public void changeName(FullName fullName) { ... }
}
```

### Identidade e Igualdade

Dois clientes são iguais se possuem o **mesmo ID**, independentemente de seus atributos:

```java
@Override
public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Customer customer = (Customer) o;
    return Objects.equals(id, customer.id);
}

@Override
public int hashCode() {
    return Objects.hashCode(id);
}
```

### Comportamentos Encapsulam Lógica de Negócio

```java
public void archive() {
    verifyIfChangeable();  // Regra: cliente não pode estar arquivado
    this.setIsArchived(true);
    this.setArchivedAt(OffsetDateTime.now());
    // Anonimiza dados pessoais
    this.setFullName(new FullName("Anonymous", "Customer"));
    this.setPhone(new Phone("000-000-0000"));
    this.setDocument(new Document("000-00-0000"));
    this.setEmail(new Email(UUID.randomUUID() + "@anonymous.com"));
    this.setBirthDate(null);
    this.setIsPromotionNotificationsAllowed(false);
    this.setAddress(this.address.toBuilder()
            .number("Anonymized")
            .complement(null).build());
}
```

O método `archive()` não apenas marca o cliente como arquivado, mas também **anonimiza os dados pessoais**, demonstrando como a lógica de negócio complexa fica encapsulada na entidade.

## <a name="rich-vs-anemic"></a>3. Rich Domain Model vs Anemic Domain Model

### ❌ Anemic Domain Model (Anti-padrão)

Entidade anêmica é um "data holder" com getters/setters públicos:

```java
// ❌ NÃO FAÇA ASSIM
@Entity
public class Customer {
    @Id private UUID id;
    private String fullName;
    private String email;
    
    // Apenas getters/setters públicos
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}

// Lógica espalhada em serviços
@Service
public class CustomerService {
    public void archive(Customer customer) {
        customer.setIsArchived(true);
        customer.setFullName("Anonymous");
        // ... lógica espalhada
    }
}
```

**Problemas:**
- Lógica de negócio espalhada em serviços
- Difícil de testar isoladamente
- Fácil violar regras de negócio acidentalmente
- Não há garantia de validações

### ✅ Rich Domain Model (Padrão recomendado)

Entidade rica encapsula regras de negócio e dados com **setters privados** e **métodos comportamentais públicos**:

```java
// ✅ FAÇA ASSIM
@Accessors(fluent = true)
@Getter
public class Customer {
    private CustomerId id;
    private FullName fullName;
    private Email email;
    private Boolean isArchived;

    // Setters privados (controle interno)
    private void setFullName(FullName fullName) {
        Objects.requireNonNull(fullName);
        this.fullName = fullName;
    }

    // Comportamentos públicos (expressam intenção de negócio)
    public void archive() {
        verifyIfChangeable();  // Valida regra
        this.setIsArchived(true);
        this.setArchivedAt(OffsetDateTime.now());
        // ... anonimização
        this.setFullName(new FullName("Anonymous", "Customer"));
    }

    public void changeName(FullName fullName) {
        verifyIfChangeable();  // Valida regra
        this.setFullName(fullName);
    }
}
```

**Benefícios:**
- ✅ Regras de negócio sempre mantidas
- ✅ Impossível deixar entidade em estado inválido
- ✅ Código autoexplicativo (intenção clara)
- ✅ Fácil de testar isoladamente

## <a name="identificadores"></a>4. Identificadores e UUIDs

### Por que evitar IDs seriais em microsserviços?

Em sistemas distribuídos, depender de IDs incrementais costuma gerar gargalos e acoplamento com o banco de dados. Na prática, isso traz alguns efeitos colaterais:

- ❌ Maior contenção no banco (ponto único para geração de ID)
- ❌ Dificuldade de escalar entre shards e múltiplas instâncias
- ❌ Exposição indireta de volume/crescimento de dados
- ❌ Mais atrito em cenários de replicação e sincronização

Por isso, o domínio usa identificadores gerados na aplicação, com boa ordenação temporal e baixa colisão.

### UUID v7 (Time-Based Epoch Random)

No projeto, o `IdGenerator` encapsula essa geração:

```java
public class IdGenerator {
    private static final TimeBasedEpochRandomGenerator generator =
        Generators.timeBasedEpochRandomGenerator();

    public static UUID generateTimeBasedUUID() {
        return generator.generate();
    }
}
```

O UUID v7 combina previsibilidade temporal com aleatoriedade:

```
┌─────────────────────┬───────────────────────────────┐
│   Timestamp (48b)   │  Random (80b)                 │
├─────────────────────┴───────────────────────────────┤
│ 2024-03-03T10:45:23.123 + 80 bits aleatórios       │
└────────────────────────────────────────────────────┘
```

✅ **Ordenável por tempo**: melhora a localidade em índices (ex.: B-tree)  
✅ **Distribuído**: pode ser gerado na aplicação, sem coordenador central  
✅ **Escalável**: reduz acoplamento com infraestrutura de persistência  
✅ **Prático para testes**: a estratégia fica centralizada no `IdGenerator`  

### Nota sobre Estratégias de ID

O domínio não usa uma única estratégia para todos os agregados. A escolha varia conforme o contexto:
- **CustomerId, ProductId** → UUID v7 (Time-Based Epoch Random)
- **OrderId, OrderItemId** → TSID (Time-based Sequential ID)

Ambas são abordagens **distribuídas** e **ordenáveis temporalmente**. O objetivo é preservar desempenho de escrita/leitura em índices e evitar dependência de um gerador central no banco.

## <a name="value-objects"></a>5. Value Objects

### Conceito

Um **Value Object** é um objeto que:
- **Não possui identidade única** - é definido por seus atributos
- É **imutável** - não muda após criação
- Representa um **conceito do domínio** (Email, Phone, Address)
- Pode conter **comportamentos e validações** (LoyaltyPoints.add())

### Implementação com Java Records

O projeto utiliza **Java Records** para Value Objects de forma concisa:

#### Email (validação simples)

```java
public record Email(String value) {
    public Email {
        FieldValidations.requiresValidEmail(value, 
            ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

**Características:**
- ✅ Validação automática no construtor compacto
- ✅ Imutável por padrão (record)
- ✅ `equals()`, `hashCode()` gerados automaticamente

#### FullName (validação e normalização)

```java
public record FullName(String firstName, String lastName) {
    public FullName {
        firstName = FieldValidations.requiresNonBlank(firstName).trim();
        lastName = FieldValidations.requiresNonBlank(lastName).trim();
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
```

#### LoyaltyPoints (com comportamento)

```java
public record LoyaltyPoints(@NonNull Integer value) 
        implements Comparable<LoyaltyPoints> {
    
    public static final LoyaltyPoints ZERO = new LoyaltyPoints(0);

    public LoyaltyPoints() {
        this(0);
    }

    public LoyaltyPoints {
        if (value < 0) {
            throw new IllegalArgumentException(
                ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE);
        }
    }

    public LoyaltyPoints add(@NonNull LoyaltyPoints points) {
        return add(points.value());
    }

    public LoyaltyPoints add(@NonNull Integer value) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_ZERO_OR_NEGATIVE);
        }
        return new LoyaltyPoints(this.value() + value);
    }

    @Override
    public int compareTo(LoyaltyPoints o) {
        return this.value().compareTo(o.value());
    }
}
```

**Características especiais:**
- ✅ Constante `ZERO` para casos comuns
- ✅ Construtor padrão (zero pontos)
- ✅ Método `add()` com validação de regra de negócio
- ✅ Implementa `Comparable` para ordenação

#### Money (valor monetário)

```java
public record Money(@NonNull BigDecimal value) implements Comparable<Money> {

    private static final RoundingMode roundingMode = RoundingMode.HALF_EVEN;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money(String value) {
        this(new BigDecimal(value));
    }

    public Money {
        value = value.setScale(2, roundingMode);
        if (value.signum() == -1) {
            throw new IllegalArgumentException();
        }
    }

    public Money multiply(@NonNull Quantity quantity) {
        if (quantity.value() < 1) {
            throw new IllegalArgumentException();
        }
        BigDecimal multiplied = this.value.multiply(new BigDecimal(quantity.value()));
        return new Money(multiplied);
    }

    public Money add(@NonNull Money money) {
        return new Money(this.value.add(money.value));
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int compareTo(Money o) {
        return this.value.compareTo(o.value);
    }
}
```

**Características especiais:**
- ✅ Usa `BigDecimal` para precisão monetária (evita erros de ponto flutuante)
- ✅ Sempre com 2 casas decimais (`setScale(2, ...)`)
- ✅ Rejeita valores negativos (invariante: dinheiro não pode ser negativo)
- ✅ Método `multiply()` com `Quantity` para cálculos corretos
- ✅ Operações matemáticas retornam novo `Money` (imutabilidade)
- ✅ Comparável para ordenações e validações

#### Quantity (quantidade de itens)

```java
public record Quantity(@NonNull Integer value) implements Serializable, Comparable<Quantity> {

    public static final Quantity ZERO = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
    }

    public Quantity add(@NonNull Quantity quantity) {
        return new Quantity(this.value + quantity.value());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(Quantity o) {
        return this.value.compareTo(o.value());
    }
}
```

**Características especiais:**
- ✅ Quantidade nunca é negativa
- ✅ Método `add()` combina quantidades
- ✅ Comparável para validações
- ✅ Implementa `Serializable` para persistência

#### CustomerId (identidade como VO)

```java
public record CustomerId(@NonNull UUID value) {
    public CustomerId() {
        this(IdGenerator.generateTimeBasedUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

**Encapsula a lógica de geração de IDs** - ao criar `new CustomerId()`, gera automaticamente UUID v7.

#### Address (VO complexo)

```java
@Builder(toBuilder = true)
public record Address(
        @NonNull String street,
        String complement,
        @NonNull String neighborhood,
        @NonNull String number,
        @NonNull String city,
        @NonNull String state,
        @NonNull ZipCode zipCode) {
    
    public Address {
        street = FieldValidations.requiresNonBlank(street).trim();
        neighborhood = FieldValidations.requiresNonBlank(neighborhood).trim();
        number = FieldValidations.requiresNonBlank(number).trim();
        city = FieldValidations.requiresNonBlank(city).trim();
        state = FieldValidations.requiresNonBlank(state).trim();
    }
}
```

**Características especiais:**
- ✅ Composto por múltiplos atributos
- ✅ Contém outro Value Object (`ZipCode`)
- ✅ `@Builder` para facilitar criação
- ✅ `toBuilder()` permite criar cópias modificadas (preserva imutabilidade)

### Benefícios dos Value Objects

✅ **Type Safety** - compilador previne erros (não pode passar `Phone` onde espera `Email`)  
✅ **Validação Encapsulada** - impossível criar VO inválido  
✅ **Semântica Clara** - código autoexplicativo (`FullName` vs `String`)  
✅ **Reutilização** - mesmos VOs em múltiplas entidades  
✅ **Testabilidade** - testa validações isoladamente  
✅ **Imutabilidade** - records são imutáveis por natureza  

### Quando Usar Value Objects?

- ✅ Atributo com **validações específicas** (Email, Phone, CPF)
- ✅ Conceito do **domínio** (Address, Money)
- ✅ Atributo **compartilhado** entre entidades
- ✅ Quer **type safety** (evitar misturar primitivos)

## <a name="agregados"></a>6. Agregados e Aggregate Root

### Conceito

Um **Aggregate** é um grupo de classes (entidades e value objects) tratados como **unidade indivisível** onde:
- ✅ A integridade de dados é consistente
- ✅ Transações envolvem apenas este agregado
- ✅ Regras de negócio são sempre mantidas

A **Aggregate Root** é a entidade que:
- É o **ponto de entrada único** para modificações
- **Garante a integridade** de todos os objetos dentro dela
- **Encapsula as regras** que envolvem múltiplos objetos

### Exemplo: Order como Aggregate Root

```java
@Accessors(fluent = true)
@Getter
public class Order {
    private OrderId id;                    // Identidade única
    private CustomerId customerId;         // Referência a outro agregado (por ID)
    private Set<OrderItem> items;          // Entidades internas
    private Money totalAmount;
    private Quantity totalItems;
    private Billing billing;
    private Shipping shipping;
    private OrderStatus status;
    // ... mais campos e timestamps

    public void addItem(@NonNull ProductId productId, @NonNull ProductName productName,
                        @NonNull Money price, @NonNull Quantity quantity) {
        // Validações que afetam o agregado
        OrderItem orderItem = OrderItem.brandNew()
                .orderId(this.id())
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
        this.items.add(orderItem);
        this.recalculateTotals();  // ← Garante consistência
    }

    public void place() {
        verifyIfCanChangeToPlaced();  // Valida invariantes
        this.setPlacedAt(OffsetDateTime.now());
        this.changeStatus(OrderStatus.PLACED);
    }

    public Set<OrderItem> items() {
        return Collections.unmodifiableSet(this.items);  // Imutável
    }
}
```

### Características e Proteções

**1. Uma Única Raiz:** `Order` é acessada diretamente. `OrderItem` nunca é acessada via repositório próprio—sempre através de `Order.

**2. Integridade de Regras:** Toda modificação passa pela raiz:
```java
// ✅ CORRETO
order.addItem(productId, productName, price, quantity);  // Passa validações

// ❌ ERRADO
order.items().add(orderItem);  // Tentativa falha (Unmodifiable)
```

**3. Referências Apenas por ID:** Agregados independentes se referenciam por ID, não por objeto direto:
```java
private CustomerId customerId;  // ✅ Referência em ID
// private Customer customer;   // ❌ Nunca isto
```

**4. Collections Sempre Imutáveis:** Retorna views imutáveis para proteger integridade:
```java
public Set<OrderItem> items() {
    return Collections.unmodifiableSet(this.items);
}
```

### Invariantes Garantidos

Um agregado mantém condições que **sempre devem ser verdadeiras**:

```java
// INVARIANTE: Totais sempre sincronizados
private void recalculateTotals() {
    // totalAmount = Sum(item.totalAmount)
    Money total = items.stream()
        .map(OrderItem::totalAmount)
        .reduce(Money.ZERO, Money::add);
    this.totalAmount = total;
}

// INVARIANTE: Data de entrega sempre no futuro
public void changeShipping(@NonNull Shipping newShipping) {
    if (newShipping.expectedDate().isBefore(LocalDate.now())) {
        throw new OrderInvalidShippingDeliveryDateException(this.id());
    }
    this.shipping = newShipping;
}

// INVARIANTE: Apenas certos estados de transição são permitidos
public void place() {
    if (!this.isDraft()) {
        throw new OrderCannotBePlacedException(this.id());
    }
    // ... transição de status
}
```

**Garantia:** É **impossível** deixar um agregado em estado inválido.

## <a name="regras-negocio"></a>7. Regras de Negócio e Comportamento

Regras de negócio vivem nas **entidades de domínio**, não em serviços. Exemplos:

### Exemplo 1: Adicionar Pontos de Lealdade

```java
public void addLoyaltyPoints(LoyaltyPoints loyaltyPointsAdded) {
    verifyIfChangeable();  // REGRA: Cliente não pode estar arquivado
    this.setLoyaltyPoints(this.loyaltyPoints.add(loyaltyPointsAdded));
}
```
**Nota:** O `LoyaltyPoints` VO também valida (não aceita zero/negativo).

### Exemplo 2: Adicionar Item ao Pedido

```java
public void addItem(@NonNull ProductId productId, 
                    @NonNull ProductName productName,
                    @NonNull Money price, 
                    @NonNull Quantity quantity) {
    OrderItem orderItem = OrderItem.brandNew()
            .orderId(this.id())
            .productId(productId)
            .productName(productName)
            .price(price)
            .quantity(quantity)
            .build();
    this.items.add(orderItem);
    this.recalculateTotals();  // REGRA: Totais sempre atualizados
}
```

### Exemplo 3: Controle de Status

```java
public void place() {
    // REGRA: Apenas pedidos em DRAFT podem ser colocados
    if (!this.isDraft()) {
        throw new OrderCannotBePlacedException(this.id());
    }
    // REGRA: Deve ter pelo menos um item
    if (this.items == null || this.items.isEmpty()) {
        throw new OrderDoesNotContainOrderItemException(this.id());
    }
    this.setPlacedAt(OffsetDateTime.now());
    this.changeStatus(OrderStatus.PLACED);
}
```

**Padrão:** Métodos com nomes expressivos (`place()`, `markAsPaid()`) expressam **intenção de negócio**.

## <a name="validacoes"></a>8. Validações e Encapsulamento

### Validação Encapsulada em Value Objects

**Cada Value Object valida a si mesmo** no construtor compacto:

```java
// Email - validação no construtor
public record Email(String value) {
    public Email {
        FieldValidations.requiresValidEmail(value, 
            ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
    }
}

// BirthDate - regra de negócio
public record BirthDate(@NonNull LocalDate value) {
    public BirthDate {
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                ErrorMessages.VALIDATION_ERROR_BIRTHDATE_FROM_FUTURE);
        }
    }
}
```

**Na entidade, setters apenas verificam non-null:**
```java
private void setEmail(Email email) {
    Objects.requireNonNull(email);  // Email já foi validado
    this.email = email;
}
```

**Benefícios:**
- ✅ **Fail-Fast** - erro no momento da criação
- ✅ **Single Responsibility** - cada VO valida a si
- ✅ **Reusabilidade** - VOs usados em múltiplas entidades
- ✅ **Type Safety** - compilador previne erros  

## <a name="excecoes"></a>9. Exceções de Domínio

Exceções de domínio representam **violações de regras de negócio** e estendem `DomainException`:

```java
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}

public class CustomerArchivedException extends DomainException {
    public CustomerArchivedException() {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED);
    }
}
```

**Uso em verificações:**
```java
private void verifyIfChangeable() {
    if (Boolean.TRUE.equals(this.isArchived)) {
        throw new CustomerArchivedException();
    }
}
```

**Vantagem:** `ErrorMessages` centraliza mensagens para fácil tradução/manutenção.

## <a name="invariantes"></a>10. Princípios de Tamanho e Consistência

### Conceitos Essenciais

- **Invariante:** Condição que **deve ser sempre verdadeira** dentro de um agregado
- **Consistency Boundary:** Limite onde os invariantes são garantidos (= o agregado)
- **Transactional Boundary:** Uma transação deve envolver **apenas um agregado**

### Decisão: Mantendo Order Pequena

```
Order Aggregate (coeso e testável):
├── Order (raiz)
├── OrderItem (interno) ← Sempre carregados juntos
├── Money, Quantity (VOs)
├── Billing, Shipping (VOs)

Fora (referenciados por ID):
├── CustomerId ← Em agregado Customer separado
├── ProductId ← Em agregado Product separado
```

**Razão:** Order deve ser consistente **dentro de si**. Customer e Product têm seu próprio ciclo de vida. **Domain Events** coordenam mudanças entre eles.

### Exemplo: Invariante de Totais

```java
private void recalculateTotals() {
    // INVARIANTE: totalAmount = Sum(item.totalAmount)
    // Isto é verificado e mantido automaticamente
    Money total = items.stream()
        .map(OrderItem::totalAmount)
        .reduce(Money.ZERO, Money::add);
    this.totalAmount = total;
}
```

**Garantia:** Impossível `totalAmount` ficar fora de sincronia com os itens.

## <a name="factories"></a>11. Factories e Builders

Factory encapsula lógica complexa de criação. Implementação com **Lombok @Builder + Static Factory Methods**:

```java
@Accessors(fluent = true)
@Getter
public class Customer {
    // Factory: novo cliente
    @Builder(builderClassName = "BrandNewCustomerBuild", builderMethodName = "brandNew")
    private static Customer createBrandNew(FullName fullName, BirthDate birthDate, 
                                           Email email, Phone phone, Document document,
                                           Boolean promotionNotificationsAllowed, Address address) {
        return new Customer(new CustomerId(),  // Gera UUID v7
                fullName, birthDate, email, phone, document,
                promotionNotificationsAllowed, false,  // não arquivado
                OffsetDateTime.now(), null,
                LoyaltyPoints.ZERO, address);
    }

    // Factory: cliente existente (reconstituição do banco)
    @Builder(builderClassName = "ExistingCustomerBuild", builderMethodName = "existing")
    private Customer(CustomerId id, FullName fullName, /* ... todos campos ... */) {
        // Inicializa todos os campos
    }
}
```

**Uso:**
```java
// Novo cliente
Customer customer = Customer.brandNew()
    .fullName(new FullName("John", "Doe"))
    .email(new Email("john@email.com"))
    .build();

// Reconstituir do banco
Customer customer = Customer.existing()
    .id(new CustomerId(UUID.fromString("...")))
    .fullName(new FullName("John", "Doe"))
    .loyaltyPoints(new LoyaltyPoints(150))
    .build();
```

### Factory com Lógica de Negócio

Às vezes é útil encapsular lógica complexa:

```java
public class OrderFactory {
    public static Order filled(CustomerId customerId, Shipping shipping,
                               Billing billing, PaymentMethod paymentMethod,
                               ProductId productId, ProductName productName,
                               Money price, Quantity quantity) {
        Order order = Order.draft(customerId);
        order.changeBilling(billing);
        order.changeShipping(shipping);
        order.changePaymentMethod(paymentMethod);
        order.addItem(productId, productName, price, quantity);
        return order;  // Pronta para ser colocada
    }
}
```

### Test Data Builder

Para testes, builders pré-configurados simplificam setup:

```java
public class OrderTestDataBuilder {
    public static Order.ExistingOrderBuilder completeOrder() {
        return Order.existing()
                .id(new OrderId())
                .customerId(new CustomerId())
                .status(OrderStatus.PLACED)
                .totalAmount(new Money("100.00"))
                .totalItems(new Quantity(2))
                // ... resto dos campos pré-configurados
                .items(new HashSet<>());
    }
}

// Uso em teste
@Test
void testChangeShipping() {
    Order order = OrderTestDataBuilder.completeOrder().build();
    // ... teste
}
```

**Benefícios:**
- ✅ **Semântica clara** (`brandNew()` vs `existing()`)
- ✅ **Type safety** (compilador garante campos)
- ✅ **Flexibilidade** (controle total com `existing()`)
- ✅ **Encapsulamento** (construtores privados)  

## <a name="transacoes"></a>12. Transações e Coordenação entre Agregados

### Princípio Fundamental: Uma Transação = Um Agregado

❌ **Não faça** (multi-agregados em uma transação):
```java
@Transactional
public void placeOrder(OrderId orderId, CustomerId customerId) {
    Order order = orderRepository.findById(orderId);
    Customer customer = customerRepository.findById(customerId);  // 2º agregado!
    order.place();
    customer.addLoyaltyPoints(new LoyaltyPoints(100));  // Viola princípio
    orderRepository.save(order);
    customerRepository.save(customer);
}
```
**Problemas:** Lock em 2 tabelas, falha em um quebra o outro, difícil de paralelizar.

✅ **Faça** (agregados isolados):
```java
@Transactional  // Transação 1: Order apenas
public void placeOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId);
    order.place();
    orderRepository.save(order);
}  // Transação fecha aqui

@Transactional  // Transação 2: Customer apenas
public void addLoyaltyPoints(CustomerId customerId, @NonNull LoyaltyPoints points) {
    Customer customer = customerRepository.findById(customerId);
    customer.addLoyaltyPoints(points);
    customerRepository.save(customer);
}
```

### Coordenação: Domain Events

```java
@Getter
public class Order {
    private List<DomainEvent> domainEvents = new ArrayList<>();

    public void place() {
        verifyIfCanChangeToPlaced();
        this.changeStatus(OrderStatus.PLACED);
        // Publica evento (sem transação com Customer)
        this.domainEvents.add(new OrderPlacedEvent(this.id(), this.customerId()));
    }
}

@EventListener  // Ouve evento em transação separada
@Transactional
public void onOrderPlaced(OrderPlacedEvent event) {
    Customer customer = customerRepository.findById(event.customerId());
    customer.addLoyaltyPoints(pointsForOrder(event));
    customerRepository.save(customer);
}
```

### Resumo: Boundaries

| Aspecto | Dentro do Agregado | Entre Agregados |
|---------|-------------------|-----------------|
| Integridade | Garantida (ACID) | Eventual (eventos) |
| Lock | Sim, 1 tabela | Não (mais rápido) |
| Atomicidade | Garantida | Manual/compensação |

Domain Events permitem **desacoplamento** entre agregados enquanto mantêm comunicação eficiente.

## 🎯 Resumo dos Padrões

| Padrão | Implementação | Benefício |
|--------|---------------|-----------|
| **Rich Domain Model** | `Customer`, `Order` com métodos | Lógica centralizada |
| **Aggregate Root** | `Order` como raiz de agregado | Integridade garantida |
| **Value Objects** | Records (Email, Money, Quantity) | Type safety + Validação |
| **Builder Pattern** | Lombok `@Builder` | Construção fluente |
| **Static Factory Method** | `brandNew()`, `existing()`, `draft()` | Intenção clara |
| **Factory com Lógica** | `OrderFactory.filled()` | Agregado completo |
| **Test Data Builder** | `OrderTestDataBuilder` | Testes simplificados |
| **Domain Exceptions** | `OrderCannotBePlacedException` | Erros específicos |
| **Imutable Collections** | `Collections.unmodifiableSet()` | Proteção de integridade |
| **Invariantes** | Verificação em métodos | Consistência garantida |
| **Single Responsibility** | Cada VO valida a si | Fail fast |
| **Domain Events** | Comunicação entre agregados | Desacoplamento |
| **Fluent Interface** | `@Accessors(fluent = true)` | API expressiva |
| **Single Source of Truth** | `ErrorMessages` | Mensagens centralizadas |
| **Transactional Boundary** | Uma transação = Um agregado | Performance + Simplicidade |

---

**📚 Este documento evolui junto com o projeto. Última atualização:** Março 2026
