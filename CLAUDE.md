# Spring Boot + Kotlin Development Guidelines

This document outlines best practices for developing Spring Boot applications with Kotlin, emphasizing KISS, YAGNI, SOLID principles, and clean code with comprehensive testing.

## Core Principles

### KISS (Keep It Simple, Stupid)
- Write straightforward, readable code
- Avoid over-engineering solutions
- Use simple data structures when complex ones aren't needed
- Prefer composition over inheritance when possible

### YAGNI (You Aren't Gonna Need It)
- Don't implement features until they're actually required
- Avoid premature abstractions
- Remove unused code and dependencies
- Focus on current requirements, not hypothetical future needs

### SOLID Principles
1. **Single Responsibility**: Each class should have one reason to change
2. **Open/Closed**: Open for extension, closed for modification
3. **Liskov Substitution**: Subtypes must be substitutable for their base types
4. **Interface Segregation**: Depend on abstractions, not concretions
5. **Dependency Inversion**: High-level modules shouldn't depend on low-level modules

## Code Structure & Organization

### Package Structure
```
src/main/kotlin/com/company/project/
├── config/           # Configuration classes
├── domain/           # Core business logic
│   ├── model/        # Domain entities
│   ├── repository/   # Repository interfaces
│   └── service/      # Business services
├── web/              # Web layer (controllers, DTOs)
├── infrastructure/   # External concerns (database, messaging)
└── shared/           # Common utilities
```

### Naming Conventions
- Use meaningful, descriptive names
- Classes: PascalCase (`UserService`, `OrderController`)
- Functions/variables: camelCase (`findUser`, `userId`)
- Constants: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- Packages: lowercase (`com.company.notes`)

## Spring Boot Best Practices

### Dependency Injection
```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    // Constructor injection preferred over field injection
}
```

### Configuration
```kotlin
@Configuration
class SecurityConfig {
    
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    
    // Prefer explicit bean definitions over @ComponentScan when possible
}
```

### Error Handling
```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
            .body(ErrorResponse(ex.message))
    }
}
```

## Kotlin Best Practices

### Data Classes
```kotlin
// Use data classes for DTOs and simple data containers
data class UserDto(
    val id: Long,
    val username: String,
    val email: String
)
```

### Null Safety
```kotlin
// Leverage Kotlin's null safety
fun findUser(id: Long): User? {
    return userRepository.findById(id).orElse(null)
}

// Use safe calls and elvis operator
val userName = user?.name ?: "Unknown"
```

### Extension Functions
```kotlin
// Use extension functions for utility methods
fun LocalDateTime.toIsoString(): String = 
    this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
```

### Sealed Classes for States
```kotlin
sealed class AuthenticationResult {
    data class Success(val user: UserDto) : AuthenticationResult()
    object InvalidCredentials : AuthenticationResult()
    object UserDisabled : AuthenticationResult()
}
```

## Database & JPA Best Practices

### Entity Design
```kotlin
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val username: String,
    
    @Column(nullable = false)
    val password: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### Repository Layer
```kotlin
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
    
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    fun findActiveUsers(): List<User>
}
```

## Testing Strategy

### Test Types
1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **E2E Tests**: Test complete user workflows

### Testing Structure
```
src/test/kotlin/
├── unit/           # Unit tests
├── integration/    # Integration tests  
└── e2e/           # End-to-end tests
```

### Unit Tests
```kotlin
@ExtendWith(MockKExtension::class)
class UserServiceTest {
    
    @MockK
    private lateinit var userRepository: UserRepository
    
    @MockK
    private lateinit var passwordEncoder: PasswordEncoder
    
    private lateinit var userService: UserService
    
    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, passwordEncoder)
    }
    
    @Test
    fun `should create user with hashed password`() {
        // Given
        val username = "testuser"
        val password = "password"
        val hashedPassword = "hashedPassword"
        
        every { passwordEncoder.encode(password) } returns hashedPassword
        every { userRepository.save(any()) } returns mockUser
        
        // When
        val result = userService.createUser(username, password)
        
        // Then
        result.username shouldBe username
        verify { passwordEncoder.encode(password) }
        verify { userRepository.save(match { it.password == hashedPassword }) }
    }
}
```

### Integration Tests
```kotlin
@SpringBootTest
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:testdb"])
class UserServiceIntegrationTest {
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Test
    fun `should persist user correctly`() {
        // Given
        val username = "integrationtest"
        val password = "password"
        
        // When
        val result = userService.createUser(username, password)
        
        // Then
        val savedUser = userRepository.findById(result.id)
        savedUser.isPresent shouldBe true
        savedUser.get().username shouldBe username
    }
}
```

### E2E Tests
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRegistrationE2ETest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `should complete user registration flow`() {
        // Given
        val username = "e2etest_${System.currentTimeMillis()}"
        
        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .param("username", username)
        )
        .andExpect(status().isOk)
        .andExpect(view().name("auth/register-success"))
        
        // Verify user can login with generated password
        // ... additional assertions
    }
}
```

## Security Best Practices

### Password Handling
```kotlin
@Service
class UserService(
    private val passwordEncoder: BCryptPasswordEncoder
) {
    
    fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }
    
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return passwordEncoder.matches(password, hashedPassword)
    }
}
```

### Input Validation
```kotlin
data class CreateUserDto(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)
```

## Performance Considerations

### Database Queries
- Use pagination for large result sets
- Implement proper indexing
- Avoid N+1 queries with `@EntityGraph` or explicit joins
- Use projections for read-only queries

### Caching
```kotlin
@Service
class UserService {
    
    @Cacheable("users")
    fun findUser(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }
    
    @CacheEvict("users", key = "#user.id")
    fun updateUser(user: User): User {
        return userRepository.save(user)
    }
}
```

## Build & Development Tools

### Required Commands
- **Build**: `./amper build` or `./gradlew build`
- **Test**: `./amper test` or `./gradlew test`
- **Run**: `./amper run` or `./gradlew bootRun`
- **Lint**: `./gradlew ktlintCheck`
- **Format**: `./gradlew ktlintFormat`

### Development Workflow
1. Write failing test first (TDD approach)
2. Implement minimal code to make test pass
3. Refactor while keeping tests green
4. Run full test suite before committing
5. Use meaningful commit messages following project conventions

## Code Quality Metrics

### Coverage Targets
- Unit tests: 80%+ coverage
- Integration tests: Cover all critical paths
- E2E tests: Cover main user journeys

### Static Analysis
- Use detekt for Kotlin static analysis
- Configure ktlint for consistent formatting
- Enable compiler warnings and treat them as errors

## Common Patterns to Avoid

### Anti-patterns
- God classes (classes with too many responsibilities)
- Primitive obsession (using primitives instead of value objects)
- Feature envy (methods using more data from other classes)
- Long parameter lists (use DTOs or builder pattern)
- Magic numbers/strings (use constants or enums)

### Spring-specific Anti-patterns
- Field injection (use constructor injection)
- Circular dependencies
- Using `@Autowired` everywhere (prefer explicit dependencies)
- Not using Spring's transaction management properly

## Monitoring & Observability

### Logging
```kotlin
private val logger = LoggerFactory.getLogger(UserService::class.java)

fun createUser(username: String): User {
    logger.info("Creating user with username: {}", username)
    try {
        val user = userRepository.save(User(username = username))
        logger.info("Successfully created user with id: {}", user.id)
        return user
    } catch (ex: Exception) {
        logger.error("Failed to create user: {}", username, ex)
        throw ex
    }
}
```

### Metrics
- Use Micrometer for application metrics
- Monitor key business metrics (user registrations, login failures, etc.)
- Set up health checks for dependencies

This document should be regularly updated as the project evolves and new patterns emerge.


### General Instructions
- Always run all the tests before finishing a task.
- Always run static code analysis before finishing a task.
