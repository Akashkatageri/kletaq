# StudyOS Android Architecture Overview

This document describes the modular, feature-based package structure and architectural guidelines enforced in the StudyOS Native Android codebase.

---

## 📁 Package Structure

`com.studyos.app`

```
com.studyos.app/
├── core/                   # Core shared framework, constants, navigation & utilities
│   ├── constants/          # Global constants and config definitions
│   ├── di/                 # Dependency injection modules (Hilt)
│   ├── navigation/         # Central navigation graph & routes
│   ├── theme/              # Design system, typography & color tokens
│   └── utils/              # Helper utilities & extensions
│
├── data/                   # Data layer (repositories, data sources & models)
│   ├── local/              # Local data storage (DataStore, Room)
│   ├── model/              # Data transfers & entity schemas
│   ├── remote/             # Firebase Auth, Firestore & network APIs
│   └── repository/         # Repository implementations
│
├── domain/                 # Business logic domain layer
│   ├── model/              # Pure domain entity models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Encapsulated business logic use cases
│
├── features/               # Feature-based presentation layer (Compose UI & ViewModels)
│   ├── focus/              # Deep focus timer feature
│   ├── friends/            # Social & peer milestones feature
│   ├── home/               # Dashboard home feature
│   ├── journey/            # Subjects, habits & study progress feature
│   ├── profile/            # User profile & settings feature
│   ├── tasks/              # Task planner & exam scheduling feature
│   └── widgets/            # Feature-specific widget controllers
│
├── widgets/                # Isolated App Widgets layer (Glance)
│   ├── data/               # Widget data providers
│   ├── state/              # Widget state management
│   ├── ui/                 # Glance Composable UI components
│   └── worker/             # Background update workers (WorkManager)
│
└── MainActivity.kt         # Single-activity entry point
```

---

## 🛡️ Architectural Principles

1. **Feature Isolation**: Code specific to a single feature lives within `features/<feature_name>/`.
2. **Data Layer Separation**: Remote Firebase code is isolated inside `data/remote/`. Local DataStore / Room logic resides in `data/local/`.
3. **Pure Domain Layer**: `domain/` contains clean interfaces and use cases independent of framework dependencies.
4. **Glance Widget Isolation**: `widgets/` remains completely decoupled from feature UI logic for clean widget lifecycle & background sync.
5. **Centralized Core**: Shared theme, navigation routes, and DI setup reside in `core/`.
