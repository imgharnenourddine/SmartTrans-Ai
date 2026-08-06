# 🚛 SmartTransAI

<p align="center">
  <img src="docs/assets/logo.png" alt="SmartTransAI Logo" width="180"/>
</p>

<p align="center">

**Smart Transportation Management Platform powered by Generative AI**

*Manage • Optimize • Assist • Analyze*

</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-red?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge\&logo=springboot)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge\&logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=for-the-badge\&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge\&logo=docker)
![AWS](https://img.shields.io/badge/AWS-Cloud-FF9900?style=for-the-badge\&logo=amazonaws)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge\&logo=redis)
![Kafka](https://img.shields.io/badge/Kafka-Event_Driven-000000?style=for-the-badge)
![Qdrant](https://img.shields.io/badge/Qdrant-Vector_DB-DC244C?style=for-the-badge)
![Spring AI](https://img.shields.io/badge/Spring_AI-LLM-6DB33F?style=for-the-badge)

</p>

---

# 📖 Overview

SmartTransAI is an intelligent transport operations management platform designed for transport companies.

The project combines a modern Transport Management System (TMS) with Generative Artificial Intelligence to help transport managers make better decisions, retrieve information faster, automate repetitive tasks and improve operational efficiency.

Unlike traditional management systems, SmartTransAI integrates an AI Copilot capable of understanding both operational data and company documents through Retrieval-Augmented Generation (RAG).

---

# 🎯 Project Objectives

The platform aims to:

* Manage transport operations efficiently.
* Centralize all operational data.
* Improve productivity through AI assistance.
* Reduce operational errors.
* Facilitate document retrieval.
* Generate intelligent insights.
* Provide a scalable cloud-native architecture.

---

# ✨ Main Features

## 👥 Identity & Access Management

* JWT Authentication
* Role-Based Access Control
* User Management
* Password Encryption
* User Profiles

Roles:

* Administrator
* Transport Manager
* Driver

---

## 🚛 Driver Management

* Driver Profiles
* Driving License Tracking
* License Expiration Alerts
* Availability Management
* Driver History

AI Examples:

> Which drivers have a valid heavy truck license?

> Show available drivers for next week.

---

## 🚚 Vehicle Management

Manage every truck in the fleet:

* Registration Number
* Capacity
* Mileage
* Fuel Type
* Insurance
* Technical Inspection
* Availability

AI can answer:

> Which truck is available?

> Which vehicles require inspection soon?

---

## 🏢 Client Management

Manage transport customers.

* Companies
* Contacts
* Addresses
* Communication Information

Mission history is automatically generated.

---

## 📦 Mission Management

The core module of SmartTransAI.

Each mission includes:

* Client
* Driver
* Vehicle
* Departure
* Destination
* Cargo
* Weight
* Schedule
* Status
* Notes

Mission Status:

* Planned
* In Progress
* Completed
* Cancelled

Before validating a mission, the AI analyzes:

* Vehicle capacity
* Driver availability
* Inspection validity
* Required documents

---

## 📁 Intelligent Document Management

Supported documents:

* Contracts
* Internal Procedures
* Technical Manuals
* Administrative Documents
* Delivery Notes
* Invoices

Files are stored in Amazon S3 (or MinIO during development).

---

## 🤖 AI Copilot

The AI assistant is the flagship feature of SmartTransAI.

It can:

* Answer operational questions.
* Search company knowledge.
* Explain KPIs.
* Summarize contracts.
* Generate reports.
* Recommend actions.
* Assist transport managers.
* Detect inconsistencies.

---

## 🧠 Retrieval-Augmented Generation (RAG)

Knowledge documents are automatically:

* Uploaded
* Parsed
* Split into chunks
* Embedded
* Indexed inside Qdrant

Users can ask:

> Summarize this contract.

> Which procedure should be followed after an accident?

> What are the payment terms?

> Show documents related to Client X.

---

## 📊 Dashboard

Real-time KPIs:

* Missions
* Fleet Status
* Available Drivers
* Delayed Missions
* Documents
* Notifications

AI automatically generates weekly summaries.

---

## 🔔 Notifications

The platform notifies users when:

* A mission is created.
* A mission changes status.
* Insurance expires.
* Technical inspection approaches.
* Documents are uploaded.

Notifications are event-driven using Apache Kafka.

---

# 🏛 System Architecture

```text
                        React
                          │
                REST API (Spring Boot)
                          │
 ┌─────────────────────────────────────────────┐
 │ IAM                                        │
 │ Missions                                   │
 │ Drivers                                    │
 │ Vehicles                                   │
 │ Clients                                    │
 │ Documents                                  │
 │ Dashboard                                  │
 │ Notifications                              │
 │ AI Copilot                                 │
 └─────────────────────────────────────────────┘
                          │
     PostgreSQL   Redis   Kafka   Amazon S3
                          │
                   Spring AI
                          │
                       Qdrant
```

---

# 💻 Technology Stack

## Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Spring Validation
* Spring AI

## Frontend

* React
* TypeScript
* Vite
* Tailwind CSS

## Database

* PostgreSQL

## AI

* Spring AI
* LangChain4j (future)
* Qdrant
* OpenAI / Mistral

## Cloud

* Amazon EC2
* Amazon S3
* IAM
* VPC

## DevOps

* Docker
* GitHub Actions

## Messaging

* Apache Kafka

## Cache

* Redis

---

# 📂 Project Structure

```text
SmartTransAI
│
├── backend
├── frontend
├── docs
├── docker
├── .github
└── README.md
```

---

# 🚀 Roadmap

## Phase 1

* Project Setup
* Authentication
* Users
* Roles

## Phase 2

* Driver Module
* Vehicle Module
* Client Module

## Phase 3

* Mission Management

## Phase 4

* Document Management

## Phase 5

* AI Copilot

## Phase 6

* RAG Integration

## Phase 7

* Dashboard

## Phase 8

* AWS Deployment

---

# 📖 Documentation

Project documentation will include:

* UML Diagrams
* ER Diagram
* API Documentation
* Deployment Guide
* Architecture Documentation

---

# 🤝 Contribution

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push your branch.
5. Open a Pull Request.

---

# 👨‍💻 Author

**Noureddine Imgharn**

Big Data & Cloud Computing Engineering Student

GitHub:
https://github.com/imgharnenourddine

---

# 📄 License

This project is released under the MIT License.

---

<p align="center">

**SmartTransAI — Building the Future of Intelligent Transport Management.**

</p>

