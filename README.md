<div align="center">

# IMTIDAD | امتداد

### University Student Project Knowledge & Continuity Platform

**Because great projects deserve a future beyond submission.**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-Desktop-blue)
![Oracle](https://img.shields.io/badge/Database-Oracle-red?logo=oracle)
![Maven](https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven)

</div>

---

## About IMTIDAD

Every year, universities produce remarkable student projects across different courses and graduation programs. Yet, many of these projects lose their value after submission or graduation, remaining scattered across personal devices, libraries, and academic archives.

**IMTIDAD (امتداد)** was created to address this challenge.

IMTIDAD is a university knowledge platform, implemented as a JavaFX desktop application, designed to preserve, organize, and connect outstanding academic projects.

It aims to transform student projects from isolated academic submissions into a **growing knowledge repository** that future generations can explore, learn from, and build upon.

Through IMTIDAD, students can discover ideas developed by previous cohorts, explore projects from earlier course offerings, understand the technologies used, and identify opportunities to continue existing work.

Beyond preservation, IMTIDAD seeks to build a **technical history of the university**, documenting how projects, ideas, technologies, and student contributions evolve over time.

> **IMTIDAD is not just about storing projects. It is about preserving knowledge and extending its impact across academic generations.**

---

## Key Features

| Feature | Description |
|---------|-------------|
| Academic Project Repository | Organize and preserve course and graduation projects. |
| Project Discovery | Explore previous projects, ideas, technologies, and student contributions. |
| Academic Review | Support project evaluation, feedback, approval, and publication workflows. |
| Project Passport | Provide a structured digital profile containing each project's information, team, and resources. |
| Project Continuation | Allow students to request the continuation or extension of previous projects. |
| Project Lineage | Connect original projects with their extensions to preserve their development history. |

---

## How IMTIDAD Works

The application follows a structured academic workflow:

**Project Creation → Documentation → Academic Review → Publication → Discovery → Continuation**

Students can document their projects and submit them for academic review.

Once approved and published, projects become part of the academic knowledge repository, where future students can discover them and propose extensions.

Approved continuations can be linked to their original projects, preserving the history of development and contributions.

---

## Application Screenshots

A preview of IMTIDAD's desktop application and its main workflows.

### Main Dashboard

<p align="center">
  <img src="screenshots/Untitled1.png" alt="IMTIDAD Main Dashboard" width="850">
</p>

### Academic Review & Project Continuation

<table>
<tr>
<td align="center" width="50%">

**Academic Review**

<img src="screenshots/lk.png" alt="Academic Review" width="420">

</td>
<td align="center" width="50%">

**Project Continuation**

<img src="screenshots/s.png" alt="Project Continuation" width="420">

</td>
</tr>
</table>

### Project Passport

<p align="center">
  <img src="screenshots/DS.png" alt="Project Passport" width="800">
</p>

---

## Technologies Used

| Technology | Purpose |
|------------|---------|
| Java 21 | Core application development |
| JavaFX & FXML | Desktop user interface |
| CSS | Interface styling |
| Oracle Database | Data storage and management |
| JDBC | Database connectivity |
| Maven | Dependency and build management |

### Software Design

IMTIDAD follows a layered architecture:

`JavaFX / FXML → Controllers → Services → Repositories → Oracle Database`

The application also demonstrates fundamental Object-Oriented Programming principles, including:

**Encapsulation · Abstraction · Inheritance · Polymorphism · Interfaces**

---

## Getting Started

### Requirements

- JDK 21
- Apache Maven
- Oracle Database

### Installation

Clone the repository:

```bash
git clone https://github.com/SarahAlnobiDev/IMTIDAD-Academic-Project-Platform.git
```

Navigate to the project directory:

```bash
cd IMTIDAD-Academic-Project-Platform
```

Configure your Oracle Database connection using:

`src/config.properties.example`

Create a local `src/config.properties` file with your database credentials.

Then build and run:

```bash
mvn clean compile
mvn javafx:run
```

> **Note:** Keep database credentials private. Do not commit your local `config.properties` file.

For detailed information about the architecture, database, implementation, and functionality, see:

**[Technical Documentation](IMTIDAD_DOCUMENTATION.md)**

---

## Development Team

**University of Science and Technology**  
Faculty of Computing and Information Technology  
Department of Software Engineering

**Developed by:**

- Sarah Alnobi — XZero
- Marwa Jameel
- Samira Al-Tabi

**2026**

---

## Project Origins | MADA

IMTIDAD evolved from **MADA (مدى)**, an earlier Database Systems course project focused on designing and implementing the academic project knowledge repository using Oracle Database, SQL, and an Oracle APEX prototype.

The concept was later extended into IMTIDAD, a Java-based desktop application developed using Object-Oriented Programming principles.

**[Explore the original MADA Database Project](https://github.com/SarahAlnobiDev/mada-database-system)**

---

<div align="center">

### IMTIDAD | امتداد

**Preserving Projects. Connecting Generations. Extending Knowledge.**

*Because the impact of a project goes beyond its submission.*

</div>
