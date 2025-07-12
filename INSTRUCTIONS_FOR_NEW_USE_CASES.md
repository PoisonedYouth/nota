# Instructions for Adding New Use Cases to Nota

This document provides detailed instructions for adding new use cases to the Nota application, following the established patterns and conventions.

## Package Structure

When adding a new use case to the Nota application, follow these guidelines for package structure:

1. **Create a dedicated package for your feature**:
   - Place all related classes in a package under `com.poisonedyouth.nota`
   - Name the package after the feature (e.g., `com.poisonedyouth.nota.tasks` for a task management feature)

2. **Required components for each use case**:
   - Domain model (entity class)
   - DTOs (Data Transfer Objects)
   - Controller
   - Service
   - Repository

## Domain Model

1. **Entity class**:
   - Create a JPA entity class with appropriate annotations
   - Use `@Entity` and `@Table` annotations
   - Include an ID field with `@Id` and `@GeneratedValue` annotations
   - Add necessary fields with appropriate JPA annotations
   - Example naming: `Task.kt` for a task management feature

```kotlin
@Entity
@Table(name = "tasks")
class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
```

## DTOs (Data Transfer Objects)

1. **Create DTOs for input and output**:
   - Create a DTO for creating new entities (e.g., `CreateTaskDto.kt`)
   - Create a DTO for representing entities to clients (e.g., `TaskDto.kt`)
   - Use data classes for DTOs

2. **DTO for creating entities**:
   - Include only the fields needed for creation
   - Use default values where appropriate
   - Example:

```kotlin
data class CreateTaskDto(
    val title: String,
    val description: String = "",
)
```

3. **DTO for representing entities**:
   - Include all fields needed by the client
   - Add utility methods as needed (e.g., formatting dates, generating previews)
   - Include a companion object with a factory method to convert from entity to DTO
   - Example:

```kotlin
data class TaskDto(
    val id: Long,
    val title: String,
    val description: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromEntity(task: Task): TaskDto {
            return TaskDto(
                id = task.id ?: -1,
                title = task.title,
                description = task.description,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
        }
    }

    fun getFormattedDate(): String {
        return updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }

    fun getDescriptionPreview(maxLength: Int = 100): String {
        return if (description.length > maxLength) {
            description.substring(0, maxLength) + "..."
        } else {
            description
        }
    }

    // No-parameter version for Java interoperability (used by Thymeleaf)
    fun getDescriptionPreview(): String {
        return getDescriptionPreview(100)
    }
}
```

## Repository

1. **Create a repository interface**:
   - Extend `JpaRepository` with the entity type and ID type
   - Add custom query methods as needed
   - Use the `@Repository` annotation
   - Example:

```kotlin
@Repository
interface TaskRepository : JpaRepository<Task, Long> {
    fun findAllByOrderByUpdatedAtDesc(): List<Task>
}
```

## Service

1. **Create a service class**:
   - Use the `@Service` and `@Transactional` annotations
   - Inject the repository
   - Implement methods for CRUD operations
   - Convert between entities and DTOs
   - Example:

```kotlin
@Service
@Transactional
class TaskService(
    private val taskRepository: TaskRepository,
) {

    fun createTask(createTaskDto: CreateTaskDto): TaskDto {
        val task = Task(
            title = createTaskDto.title,
            description = createTaskDto.description,
        )

        val savedTask = taskRepository.save(task)
        return TaskDto.fromEntity(savedTask)
    }

    fun findAllTasks(): List<TaskDto> {
        return taskRepository.findAllByOrderByUpdatedAtDesc()
            .map { TaskDto.fromEntity(it) }
    }

    fun findTaskById(id: Long): TaskDto? {
        return taskRepository.findById(id)
            .map { TaskDto.fromEntity(it) }
            .orElse(null)
    }
}
```

## Controller

1. **Create a controller class**:
   - Use the `@Controller` annotation
   - Use `@RequestMapping` to define the base path
   - Inject the service
   - Implement methods for handling HTTP requests
   - Return appropriate views and model attributes
   - Support HTMX for dynamic content updates
   - Example:

```kotlin
@Controller
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
) {

    @GetMapping
    fun listTasks(model: Model): String {
        model.addAttribute("tasks", taskService.findAllTasks())
        return "tasks/list"
    }

    @GetMapping("/new")
    fun showCreateForm(model: Model): String {
        model.addAttribute("createTaskDto", CreateTaskDto("", ""))
        return "tasks/create-form"
    }

    @PostMapping("/new")
    fun createTask(
        @ModelAttribute createTaskDto: CreateTaskDto,
        bindingResult: BindingResult,
        model: Model,
        @RequestHeader(value = "HX-Request", required = false) htmxRequest: String?,
    ): String {
        if (bindingResult.hasErrors()) {
            return if (htmxRequest != null) {
                model.addAttribute("createTaskDto", createTaskDto)
                "tasks/create-form :: form"
            } else {
                "tasks/create-form"
            }
        }

        val newTask = taskService.createTask(createTaskDto)

        return if (htmxRequest != null) {
            // HTMX Request: Return only the new task as a fragment
            model.addAttribute("task", newTask)
            "tasks/fragments :: task-card"
        } else {
            // Normal Request: Redirect to the list
            "redirect:/tasks"
        }
    }

    @GetMapping("/modal/new")
    fun showCreateModal(): String {
        return "tasks/create-modal :: modal-content"
    }

    @GetMapping("/count")
    fun getTasksCount(model: Model): String {
        val tasks = taskService.findAllTasks()
        model.addAttribute("tasks", tasks)
        return "tasks/list :: .tasks-count"
    }
}
```

## Templates

1. **Create Thymeleaf templates**:
   - Create templates in the `resources/templates` directory
   - Create a subdirectory for your feature (e.g., `tasks`)
   - Create templates for listing, creating, and viewing entities
   - Use fragments for reusable components
   - Example structure:
     - `resources/templates/tasks/list.html`
     - `resources/templates/tasks/create-form.html`
     - `resources/templates/tasks/create-modal.html`
     - `resources/templates/tasks/fragments.html`

## Testing

When adding a new use case, you must create the following tests:

### 1. Unit Tests

Create unit tests for the service class:
- Test each method in isolation
- Mock the repository
- Use descriptive test names
- Follow the Given-When-Then pattern
- Example: `TaskServiceTest.kt`

```kotlin
class TaskServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var taskService: TaskService

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskService = TaskService(taskRepository)
    }

    @Test
    fun `findAllTasks should return empty list when no tasks exist`() {
        // Given
        every { taskRepository.findAllByOrderByUpdatedAtDesc() } returns emptyList()

        // When
        val result = taskService.findAllTasks()

        // Then
        result.size shouldBe 0
    }

    @Test
    fun `findAllTasks should return list of tasks ordered by updatedAt desc`() {
        // Given
        val now = LocalDateTime.now()
        val task1 = Task(id = 1L, title = "Task 1", description = "Description 1", createdAt = now, updatedAt = now.plusHours(2))
        val task2 = Task(id = 2L, title = "Task 2", description = "Description 2", createdAt = now, updatedAt = now.plusHours(1))
        val task3 = Task(id = 3L, title = "Task 3", description = "Description 3", createdAt = now, updatedAt = now.plusHours(3))

        every { taskRepository.findAllByOrderByUpdatedAtDesc() } returns listOf(task3, task1, task2)

        // When
        val result = taskService.findAllTasks()

        // Then
        result.size shouldBe 3
        result[0].id shouldBe 3L
        result[1].id shouldBe 1L
        result[2].id shouldBe 2L
    }
}
```

### 2. Controller Tests

Create unit tests for the controller class:
- Test each endpoint
- Mock the service
- Use MockMvc for testing HTTP requests
- Verify view names and model attributes
- Example: `TaskControllerTest.kt`

```kotlin
class TaskControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var taskService: TaskService

    @BeforeEach
    fun setup() {
        taskService = mockk()
        val controller = TaskController(taskService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `listTasks should return list view with tasks`() {
        // Given
        val now = LocalDateTime.now()
        val tasks = listOf(
            TaskDto(id = 1L, title = "Task 1", description = "Description 1", createdAt = now, updatedAt = now),
            TaskDto(id = 2L, title = "Task 2", description = "Description 2", createdAt = now, updatedAt = now),
        )
        every { taskService.findAllTasks() } returns tasks

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.view().name("tasks/list"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("tasks"))
            .andExpect(MockMvcResultMatchers.model().attribute("tasks", tasks))
    }
}
```

### 3. Integration Tests

Create integration tests for the service and repository:
- Use `@SpringBootTest` with a real database
- Test the integration between service and repository
- Clean up the database before each test
- Example: `TaskIntegrationTest.kt`

```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskIntegrationTest
    @Autowired
    constructor(
        private val taskRepository: TaskRepository,
        private val taskService: TaskService,
    ) {

        @BeforeEach
        fun setup() {
            taskRepository.deleteAll()
        }

        @Test
        fun `should return empty list when no tasks exist`() {
            // When
            val tasks = taskService.findAllTasks()

            // Then
            tasks.size shouldBe 0
        }

        @Test
        fun `should create a new task`() {
            // Given
            val createTaskDto = CreateTaskDto(
                title = "New Test Task",
                description = "New Test Description",
            )

            // When
            val createdTask = taskService.createTask(createTaskDto)

            // Then
            createdTask.title shouldBe "New Test Task"
            createdTask.description shouldBe "New Test Description"

            // Verify the task was saved to the repository
            val allTasks = taskRepository.findAll()
            allTasks.size shouldBe 1
            allTasks[0].title shouldBe "New Test Task"
            allTasks[0].description shouldBe "New Test Description"
        }
    }
```

### 4. End-to-End Tests

Create end-to-end tests for the entire feature:
- Use `@SpringBootTest` with a web environment
- Test HTTP endpoints with TestRestTemplate
- Verify the application works correctly from end to end
- Example: `TaskE2ETest.kt`

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class TaskE2ETest
    @Autowired
    constructor(
        private val restTemplate: TestRestTemplate,
        private val taskRepository: TaskRepository,
        private val taskService: TaskService,
    ) {

        @LocalServerPort
        private var port: Int = 0

        @BeforeEach
        fun setup() {
            taskRepository.deleteAll()
        }

        @Test
        fun `should return 200 OK when accessing tasks endpoint`() {
            // When
            val response = restTemplate.getForEntity("http://localhost:$port/tasks", String::class.java)

            // Then
            response.statusCode shouldBe HttpStatus.OK
        }

        @Test
        fun `should create and retrieve tasks correctly`() {
            // Given
            val now = LocalDateTime.now()
            val task1 = Task(title = "E2E Test Task 1", description = "E2E Test Description 1", createdAt = now, updatedAt = now)
            val task2 = Task(title = "E2E Test Task 2", description = "E2E Test Description 2", createdAt = now, updatedAt = now.plusHours(1))
            taskRepository.saveAll(listOf(task1, task2))

            // When
            val tasks = taskService.findAllTasks()

            // Then
            tasks.size shouldBe 2

            // Verify the tasks have the expected content
            val titles = tasks.map { it.title }
            titles shouldContain "E2E Test Task 1"
            titles shouldContain "E2E Test Task 2"

            // Verify the tasks are ordered correctly (most recent first)
            tasks[0].title shouldBe "E2E Test Task 2"
            tasks[1].title shouldBe "E2E Test Task 1"
        }
    }
```

## Code Quality

Ensure your code follows the project's quality standards:

1. **Run ktlint** to check and fix code style issues:
   ```bash
   ./tools/ktlint.sh
   ```

2. **Run detekt** to perform static code analysis:
   ```bash
   ./tools/detekt.sh
   ```

3. **Install Git hooks** to automatically run ktlint and detekt before each commit:
   ```bash
   ./tools/install-hooks.sh
   ```

## Summary Checklist

When adding a new use case, ensure you have:

- [ ] Created a dedicated package for your feature
- [ ] Created a domain model (entity class)
- [ ] Created DTOs for input and output
- [ ] Created a repository interface
- [ ] Created a service class
- [ ] Created a controller class
- [ ] Created Thymeleaf templates
- [ ] Created unit tests for the service
- [ ] Created unit tests for the controller
- [ ] Created integration tests
- [ ] Created end-to-end tests
- [ ] Run ktlint and detekt to ensure code quality
- [ ] Installed Git hooks to enforce code quality
