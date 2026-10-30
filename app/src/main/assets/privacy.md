# Kletaq Privacy Policy

**Last Updated: October 31, 2026**

Your privacy and trust are paramount at Kletaq. This Privacy Policy outlines how we collect, protect, process, and respect your personal information across the Kletaq Android application, home screen widgets, cloud synchronization, and academic learning tools.

---

### 1. Information We Collect
To deliver a personalized, distraction-free academic control center, Kletaq collects:
- **Account Identity**: Your Google email address, display name, and profile avatar provided through secure Google Sign-In.
- **Academic Profile**: Your selected university (e.g., VTU), college institute, engineering branch, curriculum scheme, current semester, and enrolled/backlog subjects.
- **Study & Progress Metrics**: Daily focus session durations, completed tasks, XP earned, active streaks, study shield status, completed modules, and spaced repetition revision logs.
- **Study Notes & Documents**: Scanned classroom handwritten notes and lab manual documents you upload or attach for OCR text extraction, indexing, and active recall drills.

---

### 2. Artificial Intelligence (AI) & BYO-AI Processing
Kletaq features AI-powered study tools including Topic Exam Tutors, Practical Lab Viva Voce Drills, Syllabus Importers, and Model Paper Solvers:
- **Grounded Study Assistance**: Handwritten notes and academic doubts are processed through state-of-the-art AI foundation models (such as Google Gemini, Groq, or OpenAI) strictly to generate instant exam explanations, viva defenses, and algorithmic drills.
- **Zero AI Model Training**: Kletaq does **not** use your personal notes, handwritten documents, uploaded PDFs, or exam questions to train proprietary AI foundation models.
- **Bring-Your-Own-AI (BYO-AI) & Multi-Key Pooling**:
  - If you connect your own custom API keys (such as free Google AI Studio keys, Groq, OpenRouter, DeepSeek, or OpenAI), your keys are stored locally on your device using hardware-backed encryption (`EncryptedSharedPreferences`).
  - All BYO-AI API calls communicate directly from your device to the respective AI provider's official HTTPS endpoints.
  - Your API keys, key nicknames, and usage metrics are **never** transmitted to, stored on, logged by, or shared with Kletaq servers.
  - Multi-key auto-failover and load balancing operate strictly within the client device.

---

### 3. Google Sign-In & Firebase Cloud Services
- **Google Authentication**: When signing in, Kletaq receives only standard profile metadata (email, name, photo URL). We do not request or access your contacts, Google Drive files, calendar events, or private correspondence.
- **Cloud Database (Firestore)**: Progress data, tasks, and roadmap completions are synchronized using Google Cloud Firestore with enterprise-grade encryption in transit (TLS 1.3) and at rest (AES-256).
- **Notes Storage (Cloudflare R2)**: Uploaded handwritten classroom notes are securely stored with restricted access tied to your authenticated Firebase UID.

---

### 4. Device Integrity & App Check
To protect platform infrastructure and student accounts from unauthorized scraping, abuse, or automated bots, Kletaq utilizes Google Play Integrity and Firebase App Check to cryptographically verify authentic, unmodified app installations.

---

### 5. Local Device Features & Widgets
- **Home Screen Widgets**: Glance Android app widgets (including the Panda streak and dashboard widgets) read cached streak counts and focus state locally using Android DataStore and SharedPreferences.
- **Local Notifications**: Study reminders (morning notifications, evening streak protection, and spaced repetition alerts) are scheduled on-device via Android WorkManager without tracking your background GPS location.
- **Haptic & Audio Feedback**: Tactile vibrations and XP completion sounds operate purely on-device and collect no telemetry.

---

### 6. Safe In-App Updates
Kletaq provides seamless in-app version checks and downloads through the native Android System `DownloadManager`. Updates are downloaded to your device's standard downloads folder and verified against official release endpoints (such as GitHub Releases) without requiring invasive `REQUEST_INSTALL_PACKAGES` permissions.

---

### 7. Zero Data Selling & Advertising
We believe student academic data is sacred:
- We do **not** sell, rent, monetize, or trade your personal information, study logs, or notes to data brokers, marketers, or advertising networks.
- Kletaq does not contain third-party ad SDKs or user tracking pixels.
- Information is shared solely with essential infrastructure providers (Google Cloud Firebase, Cloudflare) strictly to host and synchronize your account.

---

### 8. Data Retention & Account Deletion
You maintain complete ownership of your academic data:
- You may log out at any time, which immediately wipes all local widget DataStore caches and cached authentication tokens from the device.
- You may request permanent deletion of your profile, study logs, notes, and cloud records by contacting our support team.

---

### 9. Contact Us
For any privacy questions, security disclosures, or data deletion inquiries, please reach out to:  
`kletaq.dev@gmail.com`
