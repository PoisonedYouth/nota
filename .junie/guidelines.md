# Project Guidelines - Nota

## Project Overview

Nota is a note-taking web application built with Spring Boot and the [Amper](https://github.com/JetBrains/amper) build tool. The application features:

- **Technology Stack**: Kotlin, Spring Boot, HTMX, Thymeleaf
- **Database Support**: H2 (development) and PostgreSQL (production)
- **Build Tool**: Amper (JetBrains' modern build tool)
- **Main Entry Point**: `com.poisonedyouth.nota.ApplicationKt`

## Project Structure

The project follows a standard Spring Boot structure with Kotlin:

```
src/com/poisonedyouth/nota/
├── Application.kt              # Application entry point
└── notes/                      # Notes feature module
    ├── CreateNoteDto.kt        # DTO for creating notes
    ├── Note.kt                 # Domain model
    ├── NoteController.kt       # Web controller
    ├── NoteDto.kt             # DTO for representation
    ├── NoteRepository.kt       # Data access layer
    └── NoteService.kt          # Business logic

test/com/poisonedyouth/nota/    # Test files mirror src structure
resources/
├── static/css/                 # Stylesheets
└── templates/                  # Thymeleaf templates
    ├── layout/                 # Layout templates
    └── notes/                  # Note-related templates
```

## Testing Guidelines

**YES, Junie should run tests** to verify correctness of proposed solutions.

### How to Run Tests

Use the standard Amper test command:
```bash
amper test
```

Or run specific test files using the `run_test` command with full paths:
- `run_test test/com/poisonedyouth/nota/notes/NoteServiceTest.kt`
- `run_test test/com/poisonedyouth/nota/notes/NoteControllerTest.kt`
- `run_test test/com/poisonedyouth/nota/notes/NoteE2ETest.kt`
- `run_test test/com/poisonedyouth/nota/notes/NoteIntegrationTest.kt`

### Testing Frameworks Used
- **JUnit 5** - Primary testing framework
- **MockK** - Mocking framework for Kotlin
- **Kotest** - Assertions and additional testing utilities
- **Spring Boot Test** - Integration testing support

### Testing Guidelines
- Only use Kotest for assertions.

## Build Process

**NO, Junie should NOT build the project** before submitting results unless specifically required. The test execution automatically handles necessary compilation.

If building is required, use:
```bash
amper build
```

## Code Style and Quality

The project enforces strict code style and quality standards:

### Code Style Tools
1. **ktlint** - Kotlin code style enforcement
2. **detekt** - Static code analysis

### Running Code Style Checks
```bash
./tools/ktlint.sh    # Check and fix style issues
./tools/detekt.sh    # Run static analysis
```

### Code Style Rules
- Follow `.editorconfig` configuration
- Use detekt rules from `tools/default-detekt-config.yml`
- Git hooks automatically run style checks on commit

### Key Style Guidelines
- Use Kotlin idioms and conventions
- Maintain consistent indentation (4 spaces)
- Follow Spring Boot naming conventions
- Use meaningful variable and function names
- Add appropriate documentation for public APIs

## Development Workflow

1. **Make Changes**: Modify source code as needed
2. **Run Tests**: Execute relevant tests to verify functionality
3. **Style Check**: Code style is automatically enforced via git hooks
4. **Integration**: Ensure changes work with existing features

## Special Considerations

- **Database**: Uses H2 for testing, PostgreSQL for production
- **Web Framework**: HTMX integration for dynamic updates
- **Templates**: Thymeleaf templating engine
- **Configuration**: Spring Boot auto-configuration with custom settings in `module.yaml`

## General Instructions
- Delete unnecessary temporary files before commiting.
- Write a new test for every bug that occurs when running the application.
