# DriveSync

DriveSync is a multi-cloud synchronization platform that enables users to connect multiple cloud storage providers through a unified synchronization folder.

The core idea behind DriveSync is simple: users often store files across multiple cloud platforms such as Google Drive and Dropbox, making file management fragmented and inefficient. DriveSync solves this problem by creating a common sync folder across connected drives. Any file operation performed within this folder is automatically synchronized across all connected cloud storage providers.

At the same time, users can continue using each cloud storage provider normally outside the sync folder without any restrictions.

---

## Problem Statement

Managing files across multiple cloud storage services can be inconvenient. Users frequently need to:

* Upload the same file to multiple drives.
* Keep files updated across different cloud platforms.
* Manually track file changes.
* Manage duplicate copies of files.

DriveSync eliminates this manual effort by providing a unified synchronization mechanism across connected cloud storage providers.

---

## How It Works

When a user connects supported cloud storage accounts:

1. DriveSync creates or identifies a dedicated synchronization folder on each connected drive.
2. The synchronization engine continuously tracks files within these folders.
3. Any change made inside a sync folder is detected.
4. The change is propagated to all connected cloud providers.
5. Metadata and synchronization information are stored for tracking and conflict resolution.

### Example

Google Drive

```text
DriveSync/
├── Resume.pdf
├── Notes.docx
└── Project.zip
```

Dropbox

```text
DriveSync/
├── Resume.pdf
├── Notes.docx
└── Project.zip
```

If a user:

* Uploads a file to the DriveSync folder in Google Drive
* Updates a file in the DriveSync folder in Dropbox
* Deletes a file from a connected sync folder

DriveSync automatically synchronizes the change across all connected drives.

---

## Key Features

### Multi-Cloud Integration

* Google Drive Integration
* Dropbox Integration
* OAuth 2.0 Authentication

### File Operations

* Upload Files
* Download Files
* Update Files
* Delete Files
* List Files
* File Metadata Retrieval

### Synchronization Engine

* Bidirectional Synchronization
* One-Way Synchronization
* Delta Synchronization
* Change Detection
* Metadata Tracking
* Conflict Resolution

### Sync Management

* Sync Status Tracking
* File Version Monitoring
* Synchronization History
* Automated File Propagation

---

## Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* MySQL
* Maven

### Frontend

* React
* Vite
* Tailwind CSS

### Cloud APIs

* Google Drive API
* Dropbox API

---

## Project Structure

```text
DriveSync
│
├── CloudSync-Backend
│   ├── Controllers
│   ├── Services
│   ├── Repositories
│   ├── Entities
│   └── Configurations
│
└── CloudSync-Frontend
    ├── Components
    ├── Pages
    ├── Services
    └── UI
```

---

## Current Development Status

🚧 Active Development

### Backend

✅ Google Drive Integration

✅ Dropbox Integration

✅ Core File Operations

✅ Metadata Management

✅ Synchronization Engine

✅ Conflict Resolution Logic

✅ Database Tracking

### Frontend

🚧 User Interface Development In Progress

🚧 Dashboard Development

🚧 File Management Screens

🚧 Synchronization Monitoring UI

---

## Future Enhancements

* Microsoft OneDrive Integration
* Real-Time Synchronization Monitoring
* Scheduled Background Synchronization
* Notification System
* Activity Logs and Analytics
* Multi-User Collaboration Features
* Additional Cloud Storage Providers

---

## Learning Outcomes

Through DriveSync, I explored:

* Cloud Storage API Integration
* OAuth 2.0 Authentication Flows
* File Synchronization Concepts
* Conflict Resolution Strategies
* Distributed Data Management
* Full-Stack Application Development
* Spring Boot and React Ecosystem

---

## Author

**Aman Jha**

Computer Science Engineer | Java & Spring Boot Developer

GitHub: https://github.com/Aman2809
