# 📖 Personal Diary Application  

![Build](https://github.com/Shubhankar-P/PersonalDiary-backend/actions/workflows/ci-cd.yml/badge.svg)
[![codecov](https://codecov.io/github/Shubhankar-P/PersonalDiary-backend/graph/badge.svg?token=7TEOI73BG7)](https://codecov.io/github/Shubhankar-P/PersonalDiary-backend)
![Quality Gate](https://img.shields.io/badge/sonarqube-passed-success)
![License](https://img.shields.io/badge/license-MIT-yellow)
![Spring Boot](https://img.shields.io/badge/SpringBoot-4.1.0-green?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?logo=postgresql)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)

A **privacy-focused diary application** built with **Spring Boot** and **PostgreSQL**, where users can securely manage their personal diary entries.

This project demonstrates industry-standard practices such as **RESTful API design, MVC architecture, authentication & authorization, unit testing, code quality monitoring, and CI-friendly tools**.  

---

## 🚀 Features  

- ✍️ **Diary Management (CRUD)** – Create, Read, Update, and Delete diary entries.  
- 🔐 **User Authentication** – Secure login with Spring Security + Basic Authentication.  
- 👥 **Role-based Authorization** –  
  - `USER`: Manage only their own diary entries.  
  - `ADMIN`: View and manage all users & their diary entries.
- 📂 **PostgreSQL + Flyway** – Relational database with version-controlled schema migrations. 
- ⚡ **RESTful APIs** – Clean API design with separation of concerns (Controller → Service → Repository).  
- 🛠 **Lombok Integration** – Boilerplate reduction for getters, setters, and constructors.  
- ✅ **Unit & Integration Testing** – JUnit 5 + Mockito for service and repository layers.  
- 📊 **Code Coverage & Quality** –  
  - [JaCoCo](https://www.jacoco.org/jacoco/) for test coverage reports.  
  - [SonarQube](https://www.sonarsource.com/products/sonarqube/) for code quality and maintainability checks.  
- ⚙️ **Configuration Management** – Used `application.yml` for cleaner configuration over `application.properties`.  

---

## 🏗️ Architecture  

The application follows **MVC (Model-View-Controller)** design principles:  

- **Controller** – Handles incoming API requests.  
- **Service** – Business logic layer.  
- **Repository** – Interacts with PostgreSQL via Spring Data JPA.
- **Entity** – Represents data models (`User`, `DiaryEntry`).  


---

## 🛠️ Tech Stack  

- **Backend**: Java 21, Spring Boot 4.1.0
- **Security**: Spring Security (Basic Authentication, Role-based Access Control)  
- **Database**: PostgreSQL, Flyway (schema migrations) 
- **Testing**: JUnit 5, Mockito, JaCoCo, SonarQube  
- **Build Tool**: Maven  
- **Other Tools**: Lombok, Git/GitHub for version control  

---

## 📡 API Endpoints  

### 🔓 Public APIs  
Accessible without authentication.  
- `POST /public/signup` → Register a new user
- `POST /public/login` → Log in and receive a JWT 
- `GET /public/health-check` → Health check endpoint  

### 👤 User APIs  
Require authentication. Regular users can only access their own data.  
- `PUT /user` → Update logged-in user details  
- `DELETE /user` → Delete logged-in user  
- `GET /users/me` → Get logged-in user details  

### 🛡️ Admin APIs  
Restricted to admin role.  
- `POST /admin/create-admin-user` → Register a new admin user  
- `GET /admin/all-users` → Get all registered users  
- `GET /admin/clear-app-cache` → Refresh the in-memory config cache

### 📖 Diary Entry APIs  
Authenticated users can manage their diary entries.  
- `POST /diary` → Create a diary entry  
- `GET /diary` → Get all diary entries of logged-in user  
- `GET /diary/id/{id}` → Get a specific diary entry
- `PUT /diary/id/{id}` → Update a diary entry
- `DELETE /diary/id/{id}` → Delete a diary entry


---

## 🧪 Testing & Quality  

- ✅ **JUnit 5 + Mockito** – Unit tests for service & repository layers.  
- 📊 **JaCoCo** – Ensures high test coverage.  
- 🧹 **SonarQube** – Ensures clean code, detects bugs, and improves maintainability.  

---

## ⚙️ Getting Started  

### Prerequisites  
- Java 21+ 
- Maven  
- PostgreSQL 18+ (running locally, or a hosted instance for production) 
- (Optional) SonarQube setup  

### Installation  
```bash
# Clone repository
git clone https://github.com/Shubhankar-P/PersonalDiary-backend.git
cd PersonalDiary-backend

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```
<!-- after -->
### Configuration
```bash
# Update your application-dev.yml (or set as environment variables) with your local PostgreSQL credentials:

spring:
datasource:
url: jdbc:postgresql://localhost:5432/diarydb
username: diary_db
password: ${DB_PASSWORD_DEV}
```
Flyway automatically creates the schema on first run — no manual table setup needed once the database itself exists.

## 📈 Future Improvements  

- ~~JWT auth instead of basic auth~~ ✅ Completed
- 🐘 ~~Migrate from MongoDB to PostgreSQL for relational data integrity~~ ✅ Completed
- 🌐 Frontend with **React + Material UI** (calendar view, image uploads)  
- ☁️ Dockerize for cloud deployment  
- 🔒 End-to-end encryption for sensitive diary entries  
- 📱 Mobile app (React Native / Flutter) support  

---

## 🤝 Project Status  

This is a **personal project** built to demonstrate backend development skills with Spring Boot and industry best practices.  
Currently maintained by me for **portfolio and resume purposes**.  

---

## 📜 License  

This project is licensed under the **MIT License**.  

---

✨ Built with **Spring Boot** & ❤️ for privacy and productivity.  
