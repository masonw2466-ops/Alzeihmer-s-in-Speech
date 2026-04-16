# Alzheimer's in Speech

### An Android application designed to assist in monitoring and detecting early signs of Alzheimer's disease through speech pattern analysis.

**Alzheimer's in Speech** records voice samples, transcribes them offline using the Vosk speech recognition engine, and provides a comprehensive medical profile for tracking health history. Built as a Capstone project, the app combines audio recording, offline transcription, and medical data collection into a single, easy-to-use Android interface.

---

## Demo

<!-- Add screenshots or a demo GIF here -->

---

## Features

| Feature | Description |
|---------|-------------|
| **Voice Recording** | One-tap audio recording with `.m4a` output using MediaRecorder for high-quality captures. |
| **Offline Transcription** | Automatic speech-to-text powered by Vosk, an open-source offline recognition engine — no internet required. |
| **Recording Management** | Browse, play back, and review all saved recordings with a built-in audio player and progress tracking. |
| **Transcript Viewer** | View full transcriptions alongside audio playback with seek controls. |
| **Medical Profile** | Store detailed medical information including personal data, medications, allergies, and physician contacts. |
| **Medical History** | Comprehensive checklist covering cardiovascular, neurological, metabolic, mental health, and family history conditions. |
| **Settings** | Configurable monitoring options including phone call monitoring and continuous listening toggles. |

---

## How to Run

This is an Android Studio project built with Kotlin and Gradle.

### Prerequisites
- **Android Studio** (Hedgehog or later recommended)
- **Android SDK** (API 35+)
- **Physical Android device** (recommended for full transcription testing)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/masonw2466-ops/Alzeihmer-s-in-Speech.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle and let dependencies download.
4. Connect a physical Android device or start an emulator.
5. Run the app: **Run > Run 'app'**

### For Command Line
1. Open your terminal in the project folder.
2. If needed, grant permission: `chmod +x gradlew`
3. Build the app: `./gradlew assembleDebug`
4. Install on a connected device: `./gradlew installDebug`

**Note:** Speech transcription requires microphone access. On emulators, concurrent microphone use between MediaRecorder and Vosk may not work — testing on a physical device is recommended.

---

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Kotlin** | Primary programming language |
| **Android SDK (API 35–36)** | Target platform |
| **Vosk** | Offline speech-to-text recognition |
| **MediaRecorder / MediaPlayer** | Audio recording and playback |
| **MVVM Architecture** | ViewModel + LiveData for UI state management |
| **SharedPreferences** | Local persistence for user settings and medical history |

---

## Project Structure

```
Alzeihmer-s-in-Speech/
├── app/
│   └── src/main/
│       ├── java/.../alzeihmersapp/
│       │   ├── MainActivity.kt            # Home screen — record button + nav
│       │   ├── RecordingsActivity.kt      # Browse and play recordings
│       │   ├── TranscriptViewActivity.kt  # View transcript + audio playback
│       │   ├── MedicalActivity.kt         # Medical profile form
│       │   ├── HistoryActivity.kt         # Medical history checklist
│       │   ├── SettingsActivity.kt        # App settings & toggles
│       │   ├── adapter/
│       │   │   └── RecordingsAdapter.kt   # ListView adapter for recordings
│       │   ├── speech/
│       │   │   └── AudioTranscriber.kt    # Vosk model loading + M4A-to-text
│       │   └── viewmodel/
│       │       └── MainViewModel.kt       # Recording state + transcription
│       ├── assets/
│       │   └── model-en-us/               # Vosk English language model
│       ├── res/                            # Layouts, drawables, strings
│       └── AndroidManifest.xml
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
```

---

## License

This project is distributed under the **MIT License**.

---

## Team

- **Adrian Ninanya**
- **Mason Williams**
- **Connor Spendlove**
- **Ethan Thompson**
- **Abbie Pitts**
