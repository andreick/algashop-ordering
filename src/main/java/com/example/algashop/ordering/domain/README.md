# Domain-Driven Design no Microsserviço de Ordering

Fundamentos de **Domain-Driven Design (DDD)** aplicados à implementação do domínio de um microsserviço.

## 📚 Índice

1. [Introdução ao Domain-Driven Design](#introducao)
2. [Entities (Domain Entities)](#entities)
3. [Rich Domain Model vs Anemic Domain Model](#rich-vs-anemic)
4. [Identificadores e UUIDs](#identificadores)
5. [Value Objects](#value-objects)
6. [Regras de Negócio e Comportamento](#regras-negocio)
7. [Validações e Encapsulamento](#validacoes)
8. [Exceções de Domínio](#excecoes)
9. [Factories](#factories)

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

Entidade rica encapsula regras de negócio e dados:

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

    // Comportamentos públicos (expressam intenção)
    public void archive() {
        verifyIfChangeable();
        this.setIsArchived(true);
        this.setArchivedAt(OffsetDateTime.now());
        this.setFullName(new FullName("Anonymous", "Customer"));
        // ... anonimização completa
    }

    public void changeName(FullName fullName) {
        verifyIfChangeable();
        this.setFullName(fullName);
    }
}
```

**Benefícios:**
- ✅ Regras de negócio sempre mantidas
- ✅ Fácil de testar diretamente
- ✅ Código autoexplicativo
- ✅ Validações garantidas pelos Value Objects

## <a name="identificadores"></a>4. Identificadores e UUIDs

### Por que não usar IDs seriais?

Em microsserviços distribuídos, IDs seriais criam problemas:

- ❌ Contenção em banco de dados (bottleneck)
- ❌ Difícil sincronizar entre shards
- ❌ Revela informações sobre volume de dados
- ❌ Problema em replicação de dados

### UUID v7 (Time-Based Epoch Random)

```java
public class IdGenerator {
    private static final TimeBasedEpochRandomGenerator generator =
        Generators.timeBasedEpochRandomGenerator();

    public static UUID generateTimeBasedUUID() {
        return generator.generate();
    }
}
```

**UUID v7 combina o melhor dos dois mundos:**

```
┌─────────────────────┬───────────────────────────────┐
│   Timestamp (48b)   │  Random (80b)                 │
├─────────────────────┴───────────────────────────────┤
│ 2024-03-03T10:45:23.123 + 80 bits aleatórios       │
└────────────────────────────────────────────────────┘
```

✅ **Ordenáveis por tempo** (primeiros 48 bits = timestamp)  
✅ **Distribuído** (gerado no cliente sem coordenação)  
✅ **Pragmático** (bom desempenho em índices B-tree)  
✅ **Determinístico** (testável com seed fixo)  

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

## <a name="regras-negocio"></a>6. Regras de Negócio e Comportamento

Regras de negócio vivem nas **entidades de domínio**, não em serviços.

### Exemplo 1: Adicionar Pontos de Lealdade

```java
public void addLoyaltyPoints(LoyaltyPoints loyaltyPointsAdded) {
    verifyIfChangeable();  // REGRA: Cliente não pode estar arquivado
    this.setLoyaltyPoints(this.loyaltyPoints.add(loyaltyPointsAdded));
}
```

**Note:**
- Recebe `LoyaltyPoints` (VO), não `Integer`
- Validação de pontos negativos encapsulada no VO
- Método `add()` do VO também valida (não aceita zero/negativo)

### Exemplo 2: Controle de Notificações

```java
public void enablePromotionNotifications() {
    verifyIfChangeable();
    this.setIsPromotionNotificationsAllowed(true);
}

public void disablePromotionNotifications() {
    verifyIfChangeable();
    this.setIsPromotionNotificationsAllowed(false);
}
```

Métodos expressam **intenção de negócio** ao invés de expor `setIsPromotionNotificationsAllowed()`.

## <a name="validacoes"></a>7. Validações e Encapsulamento

### Validação Encapsulada em Value Objects

**Cada Value Object valida a si mesmo** no construtor:

```java
// Email - validação no construtor
public record Email(String value) {
    public Email {
        FieldValidations.requiresValidEmail(value, 
            ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
    }
}

// BirthDate - regra de negócio encapsulada
public record BirthDate(@NonNull LocalDate value) {
    public BirthDate {
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                ErrorMessages.VALIDATION_ERROR_BIRTHDATE_FROM_FUTURE);
        }
    }
}
```

### Na Entidade: Setters Apenas Verificam Non-Null

```java
private void setEmail(Email email) {
    Objects.requireNonNull(email);  // Email já foi validado no VO
    this.email = email;
}

private void setBirthDate(BirthDate birthDate) {
    // Pode ser null (campo opcional)
    this.birthDate = birthDate;
}
```

**Garantia:** É **impossível** criar um Value Object inválido.

### Validações Reutilizáveis

```java
public class FieldValidations {
    public static String requiresNonBlank(@NonNull String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be blank");
        }
        return value;
    }

    public static String requiresValidEmail(@NonNull String email, String errorMessage) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return email;
    }
}
```

### Vantagens

✅ **Single Responsibility** - cada VO valida apenas a si  
✅ **Fail Fast** - erro no momento da criação  
✅ **Reusabilidade** - VOs usados em múltiplas entidades  
✅ **Type Safety** - compilador previne erros  
✅ **Testabilidade** - testa validações isoladamente  

## <a name="excecoes"></a>8. Exceções de Domínio

Exceções de domínio representam **violações de regras de negócio**.

### Hierarquia

```
RuntimeException
    └── DomainException
        └── CustomerArchivedException
```

### DomainException (Base)

```java
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Estende RuntimeException** porque:
- São exceções esperadas em lógica de negócio
- Código cliente trata especificamente
- Não obriga declaração em `throws`

### CustomerArchivedException

```java
public class CustomerArchivedException extends DomainException {
    public CustomerArchivedException() {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED);
    }
}
```

**Uso:**

```java
private void verifyIfChangeable() {
    if (Boolean.TRUE.equals(this.isArchived)) {
        throw new CustomerArchivedException();
    }
}
```

### ErrorMessages (Single Source of Truth)

```java
public class ErrorMessages {
    public static final String VALIDATION_ERROR_EMAIL_IS_INVALID = 
        "Email is invalid";
    public static final String ERROR_CUSTOMER_ARCHIVED = 
        "Customer is archived it cannot be changed";

    private ErrorMessages() {}  // Não instanciável
}
```

**Benefício:** Mensagens centralizadas, fácil traduzir ou alterar.

## <a name="factories"></a>9. Factories

### Conceito

**Factory** encapsula lógica complexa de criação quando:
- Criação envolve múltiplas etapas
- Há variações do mesmo objeto
- Criação requer contextos diferentes

### Implementação com Builder e Static Factory Methods

**Lombok @Builder** com **Static Factory Methods**:

#### Na Entidade Customer

```java
@Accessors(fluent = true)
@Getter
public class Customer {
    // Builder para NOVO cliente
    @Builder(builderClassName = "BrandNewCustomerBuild", builderMethodName = "brandNew")
    private static Customer createBrandNew(
            FullName fullName, BirthDate birthDate, Email email,
            Phone phone, Document document, 
            Boolean promotionNotificationsAllowed, Address address) {
        return new Customer(
                new CustomerId(),              // Gera UUID v7
                fullName, birthDate, email, phone, document,
                promotionNotificationsAllowed,
                false,                         // Não arquivado
                OffsetDateTime.now(),          // Registrado agora
                null,                          // Sem data arquivamento
                LoyaltyPoints.ZERO,            // Pontos = 0
                address);
    }

    // Builder para cliente EXISTENTE (reconstituição)
    @Builder(builderClassName = "ExistingCustomerBuild", builderMethodName = "existing")
    private Customer(CustomerId id, FullName fullName, /* ... todos campos ... */) {
        // Inicializa todos os campos
    }
}
```

### Uso dos Builders

#### Criar Novo Cliente

```java
Customer customer = Customer.brandNew()
    .fullName(new FullName("John", "Doe"))
    .birthDate(new BirthDate(LocalDate.of(1991, 7, 5)))
    .email(new Email("johndoe@email.com"))
    .phone(new Phone("478-256-2604"))
    .document(new Document("255-08-0578"))
    .promotionNotificationsAllowed(true)
    .address(Address.builder()
            .street("Bourbon Street")
            .number("1134")
            .neighborhood("North Ville")
            .city("York")
            .state("South California")
            .zipCode(new ZipCode("12345"))
            .build())
    .build();
// ID, registeredAt, loyaltyPoints são preenchidos automaticamente
```

#### Reconstituir Cliente Existente

```java
Customer customer = Customer.existing()
    .id(new CustomerId(UUID.fromString("...")))
    .fullName(new FullName("John", "Doe"))
    // ... todos os campos ...
    .loyaltyPoints(new LoyaltyPoints(150))
    .build();
// Controle total - útil para reconstituição do banco
```

### Test Data Builders

Para testes, criamos builders pré-configurados:

```java
public class CustomerTestDataBuilder {
    public static Customer.BrandNewCustomerBuild brandNewCustomer() {
        return Customer.brandNew()
                .fullName(new FullName("John", "Doe"))
                .birthDate(new BirthDate(LocalDate.of(1991, 7, 5)))
                .email(new Email("johndoe@email.com"))
                .phone(new Phone("478-256-2604"))
                .document(new Document("255-08-0578"))
                .promotionNotificationsAllowed(true)
                .address(Address.builder()
                        .street("Bourbon Street")
                        .number("1134")
                        .neighborhood("North Ville")
                        .city("York")
                        .state("South California")
                        .zipCode(new ZipCode("12345"))
                        .build());
    }
}
```

**Uso em testes:**

```java
@Test
void testAddLoyaltyPoints() {
    Customer customer = CustomerTestDataBuilder.brandNewCustomer().build();
    
    customer.addLoyaltyPoints(new LoyaltyPoints(10));
    
    assertThat(customer.loyaltyPoints()).isEqualTo(new LoyaltyPoints(10));
}
```

### Benefícios

✅ **Clareza Semântica** - `brandNew()` vs `existing()` expressa intenção  
✅ **Type Safety** - compilador garante campos necessários  
✅ **Conveniente** - menos parâmetros para novo cliente  
✅ **Flexível** - `existing()` permite controle total  
✅ **Testabilidade** - Test Data Builders simplificam testes  

## 📝 Notas de Implementação

### Por que não usar JPA `@Entity` na camada de domínio?

```java
// ❌ NÃO MISTURAR
@Entity  // ← Jakarta Persistence
public class Customer { /* ... */ }
```

**Razão:** Domain Entity e Persistence Entity têm responsabilidades diferentes:
- **Domain Entity**: Modela negócio, comportamentos e regras
- **Persistence Entity**: Mapeia para banco de dados

Isso mantém o domínio **independente do framework de persistência**.

### Por que Records para Value Objects?

Java Records (Java 14+) são perfeitos para VOs:
- ✅ Imutáveis por padrão (`final`)
- ✅ `equals()` e `hashCode()` automáticos
- ✅ Construtor compacto (validações inline)
- ✅ Menos boilerplate
- ✅ Semântica clara ("data carriers")

## 🎯 Resumo dos Padrões

| Padrão | Implementação | Benefício |
|--------|---------------|-----------|
| **Rich Domain Model** | `Customer` com métodos | Lógica centralizada |
| **Value Objects** | Records (Email, FullName) | Type safety + Validação |
| **Builder Pattern** | Lombok `@Builder` | Construção fluente |
| **Static Factory Method** | `brandNew()` e `existing()` | Intenção clara |
| **Test Data Builder** | `CustomerTestDataBuilder` | Testes simplificados |
| **Domain Exceptions** | `CustomerArchivedException` | Erros específicos |
| **Fluent Interface** | `@Accessors(fluent = true)` | API expressiva |
| **Single Source of Truth** | `ErrorMessages` | Mensagens centralizadas |
| **Fail Fast** | Validação no construtor | Detecção precoce |

---

**📚 Este documento evolui junto com o projeto. Última atualização:** Março 2026
