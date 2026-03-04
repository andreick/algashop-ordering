# Domain-Driven Design no Microsserviço de Ordering

Fundamentos de **Domain-Driven Design (DDD)** aplicados à implementação do domínio de um microsserviço.

---

## 📚 Índice

1. [Introdução ao Domain-Driven Design](#introducao)
2. [Entities (Domain Entities)](#entities)
3. [Rich Domain Model vs Anemic Domain Model](#rich-vs-anemic)
4. [Identificadores e UUIDs](#identificadores)
5. [Regras de Negócio e Comportamento](#regras-negocio)
6. [Validações e Encapsulamento](#validacoes)
7. [Exceções de Domínio](#excecoes)
8. [Value Objects (Futuro)](#value-objects)
9. [Factories (Futuro)](#factories)
10. [Aplicação Prática no Projeto](#aplicacao-pratica)

---

## <a name="introducacao"></a>1. Introdução ao Domain-Driven Design

**Domain-Driven Design** é uma metodologia proposta por Eric Evans que coloca o **domínio do negócio** no centro da arquitetura de software. Em vez de pensarmos em entidades apenas como tabelas de banco de dados ou estruturas de dados, pensamos em **objetos que representam conceitos importantes do negócio e que encapsulam regras de negócio**.

### Pilares do DDD

- **Domain Entity**: Objeto com identidade única que persiste ao longo do tempo
- **Value Object**: Objeto sem identidade, definido por seus atributos
- **Aggregate**: Grupo de entidades e value objects tratados como uma unidade
- **Factory**: Padrão para criar objetos complexos
- **Repository**: Abstração para persistência
- **Domain Service**: Lógica que não pertence a nenhuma entidade específica
- **Domain Exception**: Exceções que representam violações de regras de negócio

---

## <a name="entities"></a>2. Entities (Domain Entities)

### O que é uma Domain Entity?

Uma **Domain Entity** é um objeto que:
- Possui uma **identidade única** que persiste ao longo do tempo (mesmo que seus atributos mudem)
- Tem um **ciclo de vida** definido no domínio
- Sofre **mudanças de estado**
- **Encapsula regras de negócio** relacionadas ao conceito que representa
- Possui **comportamentos** específicos do domínio
- É responsável por manter a **integridade dos dados** que possui

### Exemplo: Customer Entity

Nossa `Customer` é uma Domain Entity que representa um cliente no domínio de pedidos:

```java
@Accessors(fluent = true)
@Getter
public class Customer {

    private UUID id;  // ← IDENTIDADE ÚNICA
    private String fullName;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String document;
    private Boolean isArchived;
    private Integer loyaltyPoints;  // ← REGRA DE NEGÓCIO
    private OffsetDateTime registeredAt;
    private OffsetDateTime archivedAt;

    // Comportamentos específicos do domínio
    public void addLoyaltyPoints(Integer loyaltyPointsAdded) { ... }
    public void archive() { ... }
    public void enablePromotionNotifications() { ... }
    // ...
}
```

### Características importantes:

**1. Identidade única (UUID)**
```java
@Override
public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
        return false;
    Customer customer = (Customer) o;
    return Objects.equals(id, customer.id);  // Igualdade por ID
}

@Override
public int hashCode() {
    return Objects.hashCode(id);
}
```
Dois clientes são iguais se tiverem o **mesmo ID**, independentemente de seus atributos.

**2. Comportamentos que representam ações do negócio**
```java
public void archive() {
    verifyIfChangeable();  // Regra: não pode arquivar cliente já arquivado
    this.setIsArchived(true);
    this.setArchivedAt(OffsetDateTime.now());
    // Dados sensíveis são anonimizados
    this.setFullName("Anonymous");
    this.setEmail(UUID.randomUUID() + "@anonymous.com");
    // ...
}
```

---

## <a name="rich-vs-anemic"></a>3. Rich Domain Model vs Anemic Domain Model

### ❌ Anemic Domain Model (Anti-padrão)

Uma entidade **anêmica** é basicamente um "data holder" com getters e setters públicos:

```java
// ❌ NÃO FAÇA ASSIM
@Entity
public class Customer {
    @Id
    private UUID id;
    private String fullName;
    private String email;

    // Apenas getters e setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    // ... mais getters/setters
}

// A lógica fica em classes de serviço
@Service
public class CustomerService {
    public void archive(Customer customer) {
        if (customer.getIsArchived()) {
            throw new Exception("Already archived");
        }
        customer.setIsArchived(true);
        customer.setFullName("Anonymous");
        // ...
    }
}
```

**Problemas:**
- Lógica de negócio espalhada em serviços
- Difícil de testar (precisa mockar o contexto todo)
- Não há validações garantidas
- Fácil violar regras de negócio acidentalmente

### ✅ Rich Domain Model (Padrão recomendado)

Uma entidade **rica** encapsula regras de negócio e dados:

```java
// ✅ FAÇA ASSIM
@Accessor(fluent = true)
@Getter
public class Customer {

    private UUID id;
    private String fullName;
    private Boolean isArchived;

    // Setters privados (controlam acesso)
    private void setId(UUID id) {
        Objects.requireNonNull(id);
        this.id = id;
    }

    private void setFullName(String fullName) {
        Objects.requireNonNull(fullName);
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.fullName = fullName;
    }

    // Comportamentos com regras de negócio encapsuladas
    public void archive() {
        verifyIfChangeable();  // Valida a regra
        this.setIsArchived(true);
        this.setFullName("Anonymous");
    }

    private void verifyIfChangeable() {
        if (Boolean.TRUE.equals(this.isArchived)) {
            throw new CustomerArchivedException();
        }
    }
}
```

**Benefícios:**
- ✅ Regras de negócio sempre mantidas
- ✅ Fácil de testar (testa diretamente a entidade)
- ✅ Código autoexplicativo
- ✅ Seguro (não há como violar as regras)

### Nossa implementação é Rich Domain Model ✅

Observe em `Customer.java`:
- Getters públicos (fluent, sem "get" prefix)
- **Setters privados** - você não pode mexer nos dados diretamente
- Métodos públicos representam **ações do negócio**: `archive()`, `addLoyaltyPoints()`, `changeName()`

---

## <a name="identificadores"></a>4. Identificadores e UUIDs

### Por que não usar ID seriais simples?

Em um microsserviço, não sabemos quantas instâncias teremos ou como serão distribuídas. IDs seriais criam problemas:

- ❌ Contenção em banco de dados (bottleneck)
- ❌ Difícil sincronizar entre shards
- ❌ Revela informações sobre volume de dados
- ❌ Problema se dados forem replicados

### UUID v7 (Time-Based Epoch Random) - Solução adotada

```java
public class IdGenerator {

    private static final TimeBasedEpochRandomGenerator timeBasedEpochRandomGenerator =
        Generators.timeBasedEpochRandomGenerator();

    public static UUID generateTimeBasedUUID() {
        return timeBasedEpochRandomGenerator.generate();
    }
}
```

**UUID v7 combina o melhor dos dois mundos:**

✅ **Ordenáveis por tempo** (primeiros 48 bits = timestamp)
```
┌─────────────────────┬───────────────────────────────┐
│   Timestamp (48b)   │  Random (80b)                 │
├─────────────────────┴───────────────────────────────┤
│ 2024-03-03T10:45:23.123 + 80 bits aleatórios       │
└────────────────────────────────────────────────────┘
```

✅ **Distribuído** - gerado no cliente sem coordenação  
✅ **Pragmático** - bom desempenho em índices  
✅ **Determinístico** - mesma hora + seed = mesmo ID (testável)  

### Como é usado no projeto

No construtor da `Customer`:

```java
// Criação de um novo Cliente
Customer customer = new Customer(
    IdGenerator.generateTimeBasedUUID(),  // ← Gera UUID v7
    "João Silva",
    LocalDate.of(1990, 5, 15),
    "joao@email.com",
    "11-9999-9999",
    "123.456.789-00",
    true,  // Quer receber promoções?
    OffsetDateTime.now()
);
```

---

## <a name="regras-negocio"></a>5. Regras de Negócio e Comportamento

As regras de negócio vivem nas **entidades de domínio**, não em serviços ou repositórios.

### Exemplo 1: Adicionar Pontos de Lealdade

```java
public void addLoyaltyPoints(Integer loyaltyPointsAdded) {
    verifyIfChangeable();  // REGRA: Cliente não pode estar arquivado

    // REGRA: Não pode adicionar pontos negativos
    if (loyaltyPointsAdded <= 0) {
        throw new IllegalArgumentException(
            ErrorMessages.VALIDATION_ERROR_LOYALTY_POINTS_IS_NEGATIVE
        );
    }

    // Cálculo de pontos (mais uma regra de negócio)
    this.setLoyaltyPoints(this.loyaltyPoints() + loyaltyPointsAdded);
}
```

**Fluxo de teste:**
```java
@Test
void testAddLoyaltyPoints() {
    Customer customer = createCustomer();

    // ✅ Caso válido
    customer.addLoyaltyPoints(100);
    assertEquals(100, customer.loyaltyPoints());

    // ❌ Pontos negativos não permitidos
    assertThrows(IllegalArgumentException.class,
        () -> customer.addLoyaltyPoints(-50)
    );
}
```

### Exemplo 2: Arquivamento (Regra complexa)

```java
public void archive() {
    // REGRA 1: Não pode arquivar cliente já arquivado
    verifyIfChangeable();

    // REGRA 2: Arquivado significa mudança de estado
    this.setIsArchived(true);
    this.setArchivedAt(OffsetDateTime.now());

    // REGRA 3: Anonimização de dados sensíveis
    this.setFullName("Anonymous");
    this.setPhone("000-000-0000");
    this.setDocument("000-00-0000");
    this.setEmail(UUID.randomUUID() + "@anonymous.com");
    this.setBirthDate(null);  // Apaga data de nascimento
    this.setIsPromotionNotificationsAllowed(false);  // Desativa notificações
}
```

**Por que isso é regra de negócio?**
- Define como um cliente é removido do sistema (com privacidade)
- Garante consistência dos dados após arquivamento
- Centralizado em um único lugar (a entidade)

### Exemplo 3: Controle de Notificações

```java
public void enablePromotionNotifications() {
    verifyIfChangeable();  // Regra: cliente não arquivado
    this.setIsPromotionNotificationsAllowed(true);
}

public void disablePromotionNotifications() {
    verifyIfChangeable();  // Regra: cliente não arquivado
    this.setIsPromotionNotificationsAllowed(false);
}
```

Comportamento explícito: em vez de expor `setIsPromotionNotificationsAllowed()`, expor métodos que declaram intenção de negócio.

---

## <a name="validacoes"></a>6. Validações e Encapsulamento

### Validação através de Setters Privados

Todos os setters em `Customer` são **privados** e implementam validações:

```java
private void setFullName(String fullName) {
    Objects.requireNonNull(fullName,
        ErrorMessages.VALIDATION_ERROR_FULLNAME_IS_NULL);

    if (fullName.isBlank()) {
        throw new IllegalArgumentException(
            ErrorMessages.VALIDATION_ERROR_FULLNAME_IS_BLANK);
    }

    this.fullName = fullName;
}
```

**Garantia:** Nunca será possível ter um `fullName` null ou blank após criar a entidade.

### Validação de Email

Usando um validador reutilizável:

```java
// Em FieldValidations.java
public static void requiresValidEmail(String email, String errorMessage) {
    if (!EmailValidator.getInstance().isValid(email)) {
        throw new IllegalArgumentException(errorMessage);
    }
}

// Em Customer.java
private void setEmail(String email) {
    FieldValidations.requiresValidEmail(email,
        ErrorMessages.VALIDATION_ERROR_EMAIL_IS_INVALID);
    this.email = email;
}
```

### Validação de Data de Nascimento

```java
private void setBirthDate(LocalDate birthDate) {
    if (birthDate == null) {
        this.birthDate = null;  // Permitido ser nulo
        return;
    }

    // Regra: data de nascimento não pode ser no futuro
    if (birthDate.isAfter(LocalDate.now())) {
        throw new IllegalArgumentException(
            ErrorMessages.VALIDATION_ERROR_BIRTHDATE_IN_FUTURE);
    }

    this.birthDate = birthDate;
}
```

---

## <a name="excecoes"></a>7. Exceções de Domínio

Exceções de domínio representam **violações de regras de negócio** específicas.

### Hierarquia de Exceções

```
Exception
    └── DomainException (base para toda excessão de negócio)
        └── CustomerArchivedException
```

### DomainException Base

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
- São exceções esperadas e previstas em lógica de negócio
- O código cliente deve tratar especificamente
- Não devemos obrigar declaração em throws

### CustomerArchivedException

```java
public class CustomerArchivedException extends DomainException {

    public CustomerArchivedException() {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED);
    }

    public CustomerArchivedException(Throwable cause) {
        super(ErrorMessages.ERROR_CUSTOMER_ARCHIVED, cause);
    }
}
```

**Usada quando:**
```java
private void verifyIfChangeable() {
    if (Boolean.TRUE.equals(this.isArchived)) {
        throw new CustomerArchivedException();
    }
}
```

### ErrorMessages - Single Source of Truth

```java
public class ErrorMessages {

    public static final String VALIDATION_ERROR_EMAIL_IS_INVALID =
        "Email is invalid";

    public static final String VALIDATION_ERROR_FULLNAME_IS_BLANK =
        "FullName cannot be blank";

    public static final String ERROR_CUSTOMER_ARCHIVED =
        "Customer is archived it cannot be changed";

    private ErrorMessages() {  // Não instanciável
    }
}
```

**Benefício:** Todas as mensagens em um único lugar, fácil traduzir ou mudar.

---

## <a name="value-objects"></a>8. Value Objects

### Conceito: O que é um Value Object?

Um **Value Object** é um objeto que:
- **Não possui identidade única**
- É definido **pelos seus atributos**
- É **imutável** (não muda após criação)
- Implementa `equals()` e `hashCode()` baseado em **todos os atributos**
- Pode fazer parte de uma entidade ou ser usado como um tipo de dado
- Pode ser composto por outros Value Objects
- Não existe fora do contexto de uma entidade (não tem ciclo de vida próprio)
- Dados e comportamento relacionados a um conceito específico do domínio
- Pode ter regras de negócio e validações próprias

### Exemplo futuro: Email como Value Object

```java
// ✓ RECOMENDADO (futuro)
@Value  // Lombok - torna imutável
public class Email {
    String address;

    public Email(String address) {
        if (!EmailValidator.getInstance().isValid(address)) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        // Igualdade por valor (conteúdo), não por referência
        return Objects.equals(address, email.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}

// Uso na Customer:
public class Customer {
    private Email email;  // Value Object, não String

    private void setEmail(Email email) {  // Já validado no VO
        this.email = email;
    }
}
```

### Benefícios:

✅ **Validação encapsulada** - Email sempre válido se objeto existe  
✅ **Type safety** - compilador garante que é Email, não String  
✅ **Semântica clara** - código explicita: "isto é um Email"  
✅ **Reutilizável** - Email pode ser usado em outras entidades (Supplier, Employee, etc)  
✅ **Testável** - testa validação do VO separadamente  

### Quando implementar Value Objects:

- Quando um atributo tem **validações específicas**
- Quando um atributo é **compartilhado** entre entidades
- Quando um atributo tem **comportamento associado**
- Quando ganham **semântica clara** com um nome próprio

---

## <a name="factories"></a>9. Factories (Padrão de Criação)

### Conceito

Uma **Factory** encapsula a lógica complexa de criar objetos, especialmente quando:
- A criação envolve **múltiplas etapas**
- Há **variações** do mesmo objeto
- A criação requer **dados de outras fontes**

### Exemplo futuro: CustomerFactory

```java
public class CustomerFactory {

    private final IdGenerator idGenerator;
    private final EmailValidator emailValidator;

    // Factory Method para criação padrão
    public Customer createNewCustomer(
            String fullName,
            LocalDate birthDate,
            String email,
            String phone,
            String document) {

        return new Customer(
            idGenerator.generateTimeBasedUUID(),
            fullName,
            birthDate,
            email,
            phone,
            document,
            false,  // Por padrão não quer notificações
            OffsetDateTime.now()
        );
    }

    // Factory Method para criar import de legacy
    public Customer importFromLegacySystem(LegacyCustomerDTO legacy) {
        return new Customer(
            UUID.fromString(legacy.getId()),
            legacy.getName(),
            legacy.getBirthDate(),
            legacy.getEmail(),
            legacy.getPhone(),
            legacy.getDocument(),
            legacy.acceptsEmails(),
            legacy.isActive() ? null : OffsetDateTime.now(),
            OffsetDateTime.parse(legacy.getCreatedAt()),
            legacy.isActive() ? null : OffsetDateTime.parse(legacy.getArchivedAt()),
            legacy.getPoints()
        );
    }

    // Factory Method para reconstituir do banco
    public Customer reconstructFromDatabase(CustomerRow row) {
        return new Customer(
            UUID.fromString(row.id),
            row.fullName,
            row.birthDate,
            row.email,
            row.phone,
            row.document,
            row.isPromotionNotificationsAllowed,
            row.isArchived,
            row.registeredAt,
            row.archivedAt,
            row.loyaltyPoints
        );
    }
}
```

### Uso da Factory

```java
// Criação nova
Customer newCustomer = customerFactory.createNewCustomer(
    "Maria", LocalDate.of(1995, 3, 10), "maria@email.com",
    "11-99999-9999", "123.456.789-00"
);

// Importação de legacy
Customer legacyCustomer = customerFactory.importFromLegacySystem(legacyData);

// Reconstituição do banco
Customer fromDb = customerFactory.reconstructFromDatabase(row);
```

### Benefícios

✅ **Clareza** - métodos bem nomeados explicam o contexto  
✅ **Flexibilidade** - adicionar novo tipo de criação é fácil  
✅ **Reutilização** - evita código duplicado em repositórios  
✅ **Evolução** - mudar lógica de criação em um lugar  

---

## <a name="aplicacao-pratica"></a>10. Aplicação Prática no Projeto

### Estrutura de pastas

```
domain/
├── entity/
│   └── Customer.java           ← DOMAIN ENTITY (Rich Model)
├── exception/
│   ├── DomainException.java    ← Base para exceções de domínio
│   ├── CustomerArchivedException.java
│   └── ErrorMessages.java      ← Single source of truth para mensagens
├── validator/
│   └── FieldValidations.java   ← Reutilizável validações
├── utility/
│   └── IdGenerator.java        ← Gera UUIDs v7
└── README.md  (este arquivo)
```

### Como testar entidade de domínio

```java
class CustomerTest {

    @Test
    void shouldCreateValidCustomer() {
        // Arrange
        UUID id = IdGenerator.generateTimeBasedUUID();
        LocalDate birthDate = LocalDate.of(1990, 5, 15);

        // Act
        Customer customer = new Customer(
            id, "John Doe", birthDate, "john@example.com",
            "11-99999-9999", "123.456.789-00", true,
            OffsetDateTime.now()
        );

        // Assert - Invariantes garantidos
        assertEquals("John Doe", customer.fullName());
        assertEquals(0, customer.loyaltyPoints());
        assertFalse(customer.isArchived());
    }

    @Test
    void shouldNotAllowNullName() {
        assertThrows(NullPointerException.class, () -> {
            new Customer(UUID.randomUUID(), null,
                LocalDate.now(), "email@test.com",
                "phone", "doc", true, OffsetDateTime.now());
        });
    }

    @Test
    void shouldThrowExceptionWhenArchivedCustomerIsModified() {
        Customer customer = createArchivedCustomer();

        assertThrows(CustomerArchivedException.class, () -> {
            customer.addLoyaltyPoints(50);
        });
    }

    @Test
    void shouldAnonimizeDataWhenArchived() {
        Customer customer = createCustomer(
            "Original Name", "original@email.com"
        );

        customer.archive();

        assertEquals("Anonymous", customer.fullName());
        assertTrue(customer.email().contains("anonymous.com"));
        assertNull(customer.birthDate());
    }
}
```

---

## 📖 Referências

- **Domain-Driven Design** - Eric Evans
- **Implementing Domain-Driven Design** - Vaughn Vernon
- **Building Microservices** - Sam Newman
- [RFC 4122 - Universally Unique Identifier](https://datatracker.ietf.org/doc/html/rfc4122)
- [UUID v7 Specification Draft](https://datatracker.ietf.org/doc/html/draft-ietf-uuidrev-rfc4122bis)

---

## 📝 Notas de Implementação

### Por que não usamos JPA `@Entity` nesta camada?

```java
// ❌ NÃO MISTURAR
@Entity  // ← Jakarta Persistence
@Accessors(fluent = true)
@Getter
public class Customer {
    @Id private UUID id;
    // ...
}
```

**Razão:** Domain Entity e Persistence Entity têm responsabilidades diferentes:
- **Domain Entity**: Modela conceitos de negócio
- **Persistence Entity**: Mapeia entidade para tabela de banco

Isso mantém o domínio independente do framework de persistência.
