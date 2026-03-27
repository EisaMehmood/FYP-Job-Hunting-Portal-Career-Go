Markdown
#  Career Go – Job Hunting Portal (Android App)

**Career Go** is a professional-grade mobile recruitment platform designed to bridge the gap between employers and job seekers. It offers a comprehensive digital solution for job discovery, application management, and recruitment workflows—all within a high-performance Android environment.

---

###  Overview
**Career Go** optimizes the hiring lifecycle through three core pillars:

*   **`FOR EMPLOYERS`** Efficient job lifecycle management and candidate tracking.
*   **`FOR SEEKERS`** Seamless job discovery and one-tap application submission.
*   **`REAL-TIME`** Instant synchronization for application status and alerts.

---

###  Key Features

####  Job Seeker Suite
*   **`SECURE`** Multi-factor Registration & Authentication.
*   **`PROFILE`** Dynamic user profile management.
*   **`RESUME`** Digital CV/Resume cloud storage integration.
*   **`QUERY`** Category-based advanced search filtering.
*   **`SAVED`** Personal job wishlist for future tracking.
*   **`SUBMIT`** Rapid application submission workflow.
*   **`STATUS`** Real-time visual tracking of application progress.
*   **`ALERTS`** Automated push notifications for new openings.

####  Employer Suite
*   **`RECRUIT`** Business-tier account management.
*   **`POSTING`** Dynamic job creation and live editing tools.
*   **`TALENT`** Centralized dashboard for applicant review.
*   **`DECISION`** One-click Shortlist or Rejection workflow.
*   **`UPDATE`** Instant notifications for incoming applications.

####  Administrative Control
*   **`ACCESS`** Comprehensive user and permission management.
*   **`CATALOG`** Taxonomy management for job categories.
*   **`METRIC`** System-wide activity monitoring and logs.

---

###   Technical Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java (JDK 11+) |
| **Interface** | XML / Google Material Design 3 |
| **IDE** | Android Studio Dolphin/Electric Eel+ |
| **Backend** | Firebase Realtime / Cloud Firestore |
| **Communication** | SMTP Protocol (App Password Auth) |
| **Notifications** | OneSignal REST API |
| **File Handling** | Firebase Storage & Image Picker Library |

---

###  Architecture & Structure
The project utilizes a **Modular Layered Architecture** ensuring strict Separation of Concerns:

*   **`UI-LAYER`** Material Components and XML Layout optimization.
*   **`DATA-LAYER`** Firebase SDK integration for cloud persistence.
*   **`SERVICE-LAYER`** Background handling for SMTP and Push triggers.

```text
Career-Go-App/
├── app/
│   ├── src/main/
│   │   ├── java/        # Business Logic & Model-View-Controller
│   │   ├── res/         # High-density UI Assets & Layouts
│   │   └── Manifest     # System Permissions & Activity Mapping
└── build.gradle         # Dependency & SDK Management

```
---

###  Deployment Guide

1.  **Repository Initialization**
    ```bash
    git clone [https://github.com/EisaMehmood/FYP-Job-Hunting-Portal-Career-Go-/]
    ```

2.  **`CONFIG` Firebase Setup**
    * Download `google-services.json` from the Firebase Console.
    * Inject the file into the `/app` project root.
    * Initialize **Authentication** and **Firestore** modules.

3.  **`MAIL` SMTP Integration**
    * Enable **App Passwords** in your Google Security settings.
    * Configure the `Config.java` (or equivalent) with your encrypted credentials.

4.  **`DEPLOY` Build Process**
    * Sync Gradle and resolve dependencies.
    * Run `AssembleDebug` to deploy on a physical device or emulator.

---

###  Future Roadmap

* **`SHIELD`** Biometric and Social OAuth 2.0 Integration.
* **`CONNECT`** Real-time Socket.io Chat for interview scheduling.
* **`GEO`** Proximity-based job matching using Google Maps API.
* **`ANALYTICS`** Deep-insight dashboard for recruitment trends.
* **`AI-CORE`** Machine Learning for intelligent skill-to-job mapping.

---

###  Contribution & Maintenance

I welcome contributions from the developer community. To contribute, please fork the repository and initiate a feature branch.

**Lead Developer:** **Eisa Mehmood** [**Follow on GitHub**](https://github.com/EisaMehmood/)

---

> **"Transforming the recruitment landscape through elegant, data-driven mobile engineering."** > If this project serves your needs, please support the development with a ⭐ on GitHub!
