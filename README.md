<div align="center">

# IMTIDAD | امتداد
### Academic Project Management & Continuity System

**Beyond submission, ideas continue.**

A JavaFX desktop application for preserving academic projects, managing their review process, and connecting new work to previous student contributions.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue)
![Oracle Database](https://img.shields.io/badge/Database-Oracle-red?logo=oracle)
![Maven](https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven)

</div>

---

## Overview

University projects often represent months of research, design, and development, yet much of that work becomes difficult to find after a course ends.

**IMTIDAD** is an academic project management system designed to preserve that effort and make it easier for future students to discover, reference, and build upon earlier work.

Rather than treating submission as the end of a project, IMTIDAD models a longer academic lifecycle:

**Creation → Review → Publication → Continuation**

The system aims to transform academic projects from isolated submissions into a continuously growing body of knowledge that future generations of students can explore and extend.

---

## The Problem

Every academic year, university students invest significant time and effort in developing course projects and graduation projects.

However, many of these projects become difficult to access after submission.

This creates several challenges:

- Valuable university projects become scattered across personal devices and isolated archives.
- New student teams may repeat work without knowing what has already been developed.
- Academic reviews, project ownership, and supporting materials are difficult to track consistently.
- Connections between original projects and their later extensions are often lost.
- Opportunities for academic collaboration and continuous development remain underutilized.

**IMTIDAD addresses these challenges by providing a structured environment for preserving academic projects and supporting their continued development.**

---

## Key Features

### 1. Student Accounts

IMTIDAD provides student account management functionality.

Students can:

- Register and sign in.
- Manage their academic projects.
- Associate projects with their accounts.
- Track the review status of submitted projects.
- Explore previously published projects.

### 2. Academic Project Management

The system supports two primary project categories:

- **Course Projects**
- **Graduation Projects**

Students can create and manage projects while recording essential information such as:

- Project title and description.
- Project category.
- Team members and contributors.
- Technologies used.
- Academic reviewer.
- Supporting attachments.
- GitHub repository links.

### 3. Academic Review Workflow

Projects undergo an academic review process before publication.

Students submit their projects for evaluation by an assigned academic reviewer.

Reviewers can examine project details, provide feedback, and make review decisions.

The workflow supports project states such as:

- `DRAFT`
- `PENDING_REVIEW`
- `NEEDS_CHANGES`
- `APPROVED`
- `PUBLISHED`

This process helps maintain an organized academic repository rather than an unrestricted collection of project submissions.

### 4. Project Passport

Each project has a dedicated information view called the **Project Passport**.

It brings together important project information, including:

- Project identity and description.
- Project contributors.
- Technologies and tools.
- Related resources and attachments.
- Academic review information.
- Relationships with other projects.

The Project Passport serves as a structured digital identity for each academic project.

### 5. Project Continuation

One of IMTIDAD's central concepts is enabling students to continue developing earlier academic projects.

Instead of starting an entirely new project, students can explore previous work and request to build upon it.

This supports:

- Reusing previous academic efforts.
- Extending existing functionality.
- Improving earlier implementations.
- Encouraging collaboration across student generations.
- Preserving connections between original projects and subsequent developments.

### 6. Project Lineage

**Project Lineage** represents the relationships between original projects and their later developments.

It helps illustrate how academic ideas evolve over time.

For example:

```text
          Hospital Management System
                     |
          +----------+----------+
          |                     |
          v                     v
   Patient Mobile App    Appointment System
          |                     |
          v                     v
    AI Assistant        Scheduling Improvements
```

This concept supports one of IMTIDAD's primary objectives:

**Academic knowledge should continue growing rather than disappearing after submission.**

---

## Project Lifecycle

IMTIDAD organizes academic projects through a structured lifecycle.

```text
             Create Project
                    |
                    v
                  DRAFT
                    |
                    v
             PENDING_REVIEW
                    |
          +---------+---------+
          |                   |
          v                   v
    NEEDS_CHANGES          APPROVED
          |                   |
          v                   v
    Revise Project         PUBLISHED
          |                   |
          v                   v
      Resubmit          Project Discovery
                              |
                              v
                      Continuation Request
                              |
                              v
                       Project Extension
```

The system separates student and academic reviewer workflows.

Additional roles are represented in the system design, although not all have complete user-facing functionality in the current version.

---

## Technologies Used

IMTIDAD was developed using the following technologies:

| Technology | Purpose |
|------------|---------|
| Java 21 | Core application development |
| JavaFX 21.0.2 | Desktop graphical user interface |
| FXML | Declarative interface layouts |
| CSS | User interface styling |
| Oracle Database | Relational database management |
| JDBC (ojdbc11) | Java database connectivity |
| Apache Maven | Dependency management and build automation |
| SQL | Database schema and data operations |

The application combines database management, desktop interface development, and object-oriented programming principles.

---

## System Architecture

IMTIDAD follows a layered architecture designed to separate responsibilities and improve code organization.

```text
+----------------------------------+
|         JavaFX / FXML            |
|        User Interface            |
+----------------------------------+
                 |
                 v
+----------------------------------+
|           Controllers            |
|       UI Event Handling          |
+----------------------------------+
                 |
                 v
+----------------------------------+
|            Services              |
|         Business Logic           |
+----------------------------------+
                 |
                 v
+----------------------------------+
|        Repository / DAO          |
|          Data Access             |
+----------------------------------+
                 |
                 v
+----------------------------------+
|         Oracle Database          |
|         Data Persistence         |
+----------------------------------+
```

### Architecture Components

**Presentation Layer**

Responsible for displaying application screens and handling user interactions through JavaFX and FXML.

**Controller Layer**

Connects user interface events to the application's business operations.

**Service Layer**

Implements application logic, validation, and project management workflows.

**Data Access Layer**

Uses repositories and DAO components to communicate with the database.

**Database Layer**

Stores users, projects, reviews, relationships, and supporting academic information.

This separation improves maintainability and supports further development.

---

## Object-Oriented Programming

IMTIDAD was developed with an emphasis on applying Object-Oriented Programming (OOP) concepts in a practical application.

### Encapsulation

Application data and related operations are organized within classes, with controlled access to internal state.

### Abstraction

The system defines shared abstractions for common behaviors and entities.

For example, the project model includes a shared `Project` abstraction.

### Inheritance

Specialized project classes extend common project functionality.

Examples include:

- `Project`
- `CourseProject`
- `GraduationProject`

### Polymorphism

The application supports working with specialized project types through their shared abstractions.

### Interfaces

Interfaces define contracts between components and support separation of responsibilities.

Together, these principles contribute to the application's modular design.

---

## Repository Structure

The repository contains the application's source code, interface resources, database-related files, and development documentation.

```text
IMTIDAD-Academic-Project-Platform/
│
├── src/
│   ├── com/
│   │   └── imtidad/
│   │
│   └── config.properties.example
│
├── pom.xml
│
├── compile.ps1
│
├── run.ps1
│
├── validate_fxml.ps1
│
├── VALIDATION_REPORT.txt
│
├── IMTIDAD_DOCUMENTATION.md
│
├── .gitignore
│
└── README.md
```

### Important Note

This project uses a customized Maven source directory configuration.

The `src/` directory is configured in `pom.xml`.

Therefore, the existing structure should not be reorganized without updating the build configuration.

---

## Getting Started

### Prerequisites

Before running IMTIDAD, ensure the following are installed:

- JDK 21
- Apache Maven
- Oracle Database
- An IDE such as IntelliJ IDEA (optional)

A working Oracle Database connection is required.

### Step 1: Clone the Repository

```bash
git clone https://github.com/SarahAlnobiDev/IMTIDAD-Academic-Project-Platform.git
```

Navigate to the project directory:

```bash
cd IMTIDAD-Academic-Project-Platform
```

### Step 2: Configure the Database

Create a local configuration file:

```text
src/config.properties
```

Use the provided template:

```text
src/config.properties.example
```

Configure your Oracle Database connection:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.user=YOUR_ORACLE_USER
db.password=YOUR_ORACLE_PASSWORD
```

Replace the placeholder values with your local database configuration.

Alternatively, database credentials can be supplied through environment variables:

```text
ORACLE_URL
ORACLE_USER
ORACLE_PASSWORD
```

**Security Note:**

Never commit real database credentials to a public repository.

The local `src/config.properties` file is excluded through `.gitignore`.

### Step 3: Build the Project

From the project directory, execute:

```bash
mvn clean compile
```

### Step 4: Run the Application

Start the JavaFX application using:

```bash
mvn javafx:run
```

Alternatively, open the project in IntelliJ IDEA, configure JDK 21 and the required dependencies, and run:

```text
com.imtidad.Main
```

Make sure the Oracle Database is properly configured before starting the application.

---

## Technical Documentation

Detailed documentation is available in:

**[IMTIDAD_DOCUMENTATION.md](IMTIDAD_DOCUMENTATION.md)**

The documentation provides further information about:

- System functionality.
- Application architecture.
- Database design.
- Object-oriented programming implementation.
- Classes and interfaces.
- Academic review workflows.
- Project continuation mechanisms.
- Testing and validation scenarios.

---

## Current Scope

The current implementation focuses primarily on:

- Student account management.
- Academic project management.
- Project review workflows.
- Project information and documentation.
- Project continuation requests.
- Relationships between academic projects.
- Oracle Database integration.
- JavaFX desktop interfaces.

The application is an academic desktop software project and is not currently presented as a publicly hosted online platform.

---

## Future Development

IMTIDAD provides a foundation for further improvements.

Potential future enhancements include:

- Expanded administrator functionality.
- Additional visitor features.
- Advanced project search and filtering.
- Centralized project file storage.
- Notifications for project reviews and continuation requests.
- More interactive project lineage visualization.
- Project analytics and reporting.
- Stronger security and password management.
- Additional collaboration features.

These represent possible future improvements rather than functionality claimed to be fully implemented in the current version.

---

## Project Vision

IMTIDAD is built around a simple idea:

**A university project should not lose its value when the semester ends.**

By organizing academic projects, documenting their development, and connecting future contributions to previous efforts, IMTIDAD aims to support a more collaborative and sustainable academic environment.

The long-term vision is to encourage universities to treat student projects as valuable academic resources that can continue evolving across generations.

---

## Project Origins: MADA | مدى

IMTIDAD builds upon an earlier academic project called **MADA (مدى)**.

MADA was initially developed as a **Database Systems course project**, focusing on relational database design, SQL implementation, and an Oracle APEX prototype.

The concept was subsequently developed into **IMTIDAD**, a Java-based desktop application that applies object-oriented programming principles and introduces application-level workflows for managing academic projects.

The original database project is available here:

**[MADA — Database System](https://github.com/SarahAlnobiDev/mada-database-system)**

---

<div align="center">

### IMTIDAD | امتداد

**Beyond submission, ideas continue.**

Preserving academic efforts. Connecting ideas. Extending knowledge.

</div>
