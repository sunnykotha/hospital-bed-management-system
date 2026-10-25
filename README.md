# Hospital Bed Management System

A Java 21 and Maven project for hospital bed allocation, patient transfers, bed-state management, admission prioritization, and cleaning workflows.

## Prerequisites

- Java Development Kit (JDK) 21
- Git (only required for cloning the repository)

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## Clone the repository

```powershell
git clone https://github.com/sunnykotha/hospital-bed-management-system.git
Set-Location hospital-bed-management-system
```

## Verify Java

```powershell
java -version
```

Confirm that Java 21 is being used.

## Run the tests

On Windows, run:

```powershell
.\mvnw.cmd clean test
```

This cleans previous build output, compiles the project, and runs the automated test suite.

## Build the project

```powershell
.\mvnw.cmd clean package
```

## Project structure

- `src/main/java` — application source code
- `src/test/java` — automated tests
- `pom.xml` — Maven project configuration
- `mvnw.cmd` and `.mvn/` — Maven Wrapper files

## Implemented functionality

- Bed lifecycle and state transitions
- Ward acuity matching and room constraints
- Isolation and gender-sharing restrictions
- Admission allocation, prioritization, boarding, and escalation
- ICU step-down transfer reservations, completion, and cancellation
- Bed cleaning, including terminal cleaning for isolation patients
- Bed swaps and concurrent bed-assignment protections

## Test status

Run the test command above to verify the current state of the project. Test results can change as the code is modified.
