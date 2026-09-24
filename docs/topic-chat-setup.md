# Topic tutor setup

The node sheet and lesson screen open the same topic chat. It uses Firebase AI Logic's Gemini Developer API backend, with `gemini-3.1-flash-lite`. Model requests include only the selected topic's bundled notes, subject/module titles, the question, and up to 12 recent messages. No profile, personal notes, email or task records are sent to Gemini.

## Firebase console

1. In project `kletaq`, open AI Logic and complete the Gemini Developer API setup. Do not put a Gemini secret in Android resources. Check the provider's quota/billing configuration before enabling public use.
2. Register the Android app with App Check using Play Integrity and its signing certificate. The application initializes Play Integrity by default. For local emulator testing only, build `:app:assembleBenchmark -PappCheckDevelopment=true` and register that installation's debug token in Firebase App Check. Without this explicit flag, benchmark keeps Play Integrity. Never distribute the opted-in APK or commit its debug token. Do not disable production enforcement to test.
3. Merge the rule below into the existing Firestore rules. Review any broader `users` wildcard: overlapping rules are ORed, so a permissive wildcard can override this ownership constraint. Do not replace unrelated rules.

```text
match /users/{uid}/topicChats/{chatId}/messages/{messageId} {
  allow read: if request.auth != null && request.auth.uid == uid;
  allow create, update: if request.auth != null && request.auth.uid == uid
    && request.resource.data.keys().hasOnly(['id', 'role', 'text', 'createdAt', 'topicId', 'subjectId'])
    && request.resource.data.id == messageId
    && request.resource.data.role in ['user', 'model']
    && request.resource.data.text is string
    && request.resource.data.text.size() > 0
    && request.resource.data.text.size() <= 30000
    && request.resource.data.createdAt is int
    && request.resource.data.topicId is string
    && request.resource.data.subjectId is string;
  allow delete: if request.auth != null && request.auth.uid == uid;
}
```

Messages use a SHA-256 topic document key to handle legacy IDs with slashes. Each message retains its original topic/subject ID. The latest 60 messages load; only 12 bounded messages are sent to Gemini. A successful question/answer pair is written atomically with stable message IDs, making save retries safe. Chat text is untrusted user data, not proof of learning or grounds for awarding XP.

## Verification

## Rate limiting

The local guard allows five request attempts per rolling 60 seconds per signed-in account across all topic chats on this device. Failed requests count, to prevent retry storms. Saved limits survive closing the chat and restarting the app. This is NOT tamper-proof or cross-device enforcement.

Required server configuration (not deployed by this code): in Google Cloud Console for project kletaq, open Firebase AI Logic API → Manage → Quotas & System Limits. Set the applicable “Generate content requests” per-user per-region per-minute quota to 5. Do not change the unrelated default row or the project-wide quota. See https://firebase.google.com/docs/ai-logic/quotas for quota scope. This applies to all apps using AI Logic in the project and is not a daily spending cap.

Build and install only `:app:assembleBenchmark`. Verify an authenticated user can open a Python topic, submit a question and see a streamed answer based on its notes; close/reopen and confirm saved messages return. Open another topic and confirm its history is separate. Sign out/switch accounts and confirm no previous conversation appears. Test network failure, disabled AI Logic, denied Firestore access and repeated Send taps. Confirm the lesson timer stays in its existing composition behind the dialog. No automatic API requests run just from opening the chat.

The UI limits each question to 2,000 characters and response generation to 1,800 tokens. These are request-size controls, not a server-enforced daily spending cap. Set service quotas and monitoring in Firebase/Google Cloud before public rollout.
