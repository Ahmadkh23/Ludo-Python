# Security design – Question manager and question data

The assignment says the question management wizard is for system admins and exposes the answer bank,
so it must be protected from regular players. The team has to choose how, justify the choice, and implement it in code.
This document is that justification.

## 1. Threat model

| Who | What they might try | Protection |
|---|---|---|
| A player at the same computer | Open the question manager to see the answers | Admin password (§2) |
| A player at the same computer | Open the question file in Notepad to read the answers or change them | File encryption + tamper detection (§3) |
| A player at the same computer | Guess the admin password | Lockout after 5 wrong attempts (§2) |
| Admin walks away from the screen | Someone else uses the open wizard | Session expiry, Log out clears the screen (§2) |
| A bug or a shortcut in the UI | Reach admin functions without logging in | The check is in the service layer, not only in the UI (§4) |

**Out of scope:** someone with the source code and a debugger, or an administrator of the operating system.
A desktop application cannot protect data from the owner of the machine; that would need a server.
This is stated honestly here rather than claiming more than the design gives.

## 2. Admin authentication – `security/AuthService.java`

- **No default password.** On the first visit to the question manager the admin *creates* the password, so no
  shared password like `admin/admin` exists that every student knows.
- **Password rules:** at least 8 characters, letters and digits (`AuthService.validateNewPassword`).
- **Storage:** only a **salted PBKDF2-HMAC-SHA256 hash** (120,000 iterations, random 16-byte salt) is saved in
  `admin.properties`. The plain password is never written to disk (`PasswordHasher`). Password arrays are
  cleared from memory after use.
- **Constant-time comparison** (`MessageDigest.isEqual`) so the check does not leak timing information.
- **Brute-force protection:** 5 wrong passwords lock the login for 60 seconds.
- **Sessions:** a successful login returns an `AdminSession` with a random 192-bit token that **expires after
  15 minutes**. *Log out* ends the session and removes the question table (with the answers) from the screen.
- **Changing the password** requires a valid session *and* the current password.

**Why a password and not, for example, a hidden key combination or a command-line flag?**
A hidden shortcut is "security through obscurity": once one student finds it, everybody knows it.
A personal password that each admin sets, stored as a salted slow hash, is the standard, testable solution
and needs no extra infrastructure.

## 3. Protecting the question file – `security/CryptoBox.java`, `persistence/EncryptedQuestionRepository.java`

- The bank is stored as `questions.dat`, encrypted with **AES-256-GCM** (random salt and IV for every save).
  Opening it in a text editor shows only Base64 text, so the answers can't be read.
- GCM is **authenticated encryption**: any change to the file is detected. On start the game reports that the
  file was changed outside the game and offers to restore the original questions (keeping the damaged file as
  a backup) instead of using manipulated data.
- Saves are **atomic** (write a temporary file, then move it), so a crash can't leave a half-written bank.

**Limitation (honest):** the encryption key is derived from a value inside the application, because the game must
read questions during play without asking for the admin password. This stops casual reading and editing, which
is the realistic risk in a classroom. It does not stop someone who reverse-engineers the program. The real fix for
that is a server, which is outside the scope of this offline desktop project.

## 4. Enforcement in the logic layer – `service/QuestionService.java`

The UI hides the wizard, but the **service** is what enforces the rule:

| Operation | Who may call it |
|---|---|
| `draw(level)` – one random question during play | the game (players) |
| `countByLevel()` – numbers only, no answers | the start screen |
| `listAll`, `add`, `update`, `delete` | only with a valid `AdminSession` (`auth.requireValid(session)` on every call) |

The game engine only sees the narrow `QuestionProvider` interface (`draw`), never the full bank.
An `AdminSession` can only be created by `AuthService` (package-private constructor).
Even if a future screen forgot to ask for the password, the service would refuse the operation.

## 5. Data integrity rules

- The bank must keep **at least 30 questions** and at least one question per level; the service refuses changes
  that would break this (`QuestionService.delete/update`).
- Every question must have non-empty text, exactly 4 **different** answers and one correct answer (`Question` record).

## 6. Tests

`src/test/java/com/eduludo/security/SecurityTest.java` and `service/QuestionServiceTest.java` check:
the hash-only storage, right and wrong passwords, weak-password rejection, one-time setup, the lockout and its
expiry, session expiry, password change, encryption round-trip (including Hebrew text), tamper detection, a wrong key,
and that every admin operation is refused without a valid session.

