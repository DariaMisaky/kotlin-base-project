# Communication Rules

## Language Mirroring

Mirror the user's language in responses:

- User writes in **English** → respond in **English**
- User writes in **Romanian** → respond in **Romanian**

## How to Detect

Use the dominant language of the user's **most recent message** to choose the response language. Switch immediately when the user switches — do not lag a turn behind.

## Mixed-Language Messages

If the user mixes languages in a single message (e.g., Romanian text with English technical terms):

1. Default to the language used for the actual instruction/request (not the embedded code/file names)
2. If still ambiguous, fall back to the conversation's prior language
3. If no prior turns exist (fresh conversation), default to English

Example:
> "fix `BaseViewModel.performApiCall` să rethrow `CancellationException`"

Surrounding prose is Romanian → respond in Romanian. The English identifiers stay as-is (they're code).

## What This Rule Does NOT Affect

These always stay in English regardless of conversation language:

- **Code identifiers** (class names, function names, variables, package names)
- **File names** and directory names
- **Commit messages** and PR titles/descriptions (project convention)
- **Code comments** in source files (rare since the project prefers no comments)
- **String resources** in `values/strings.xml` (project is English-only — see `.claude/rules/localization.md`)
- **Log messages**, analytics events, error codes (internal/technical strings)

Only the **conversational prose** in chat responses mirrors the user's language.

## Examples

### English in → English out
> User: "fix the cancellation bug in BaseViewModel"
> Response: "Adding `catch (CancellationException) { throw e }` before the `Throwable` catch in `performApiCall`. Updating now."

### Romanian in → Romanian out
> User: "fă fix la bug-ul de cancellation"
> Response: "Adaug `catch (CancellationException) { throw e }` înainte de catch-ul pe `Throwable` în `performApiCall`. Aplic acum."

### Switch mid-conversation
> User (turn 1, Romanian): "rulează testele"
> Response (Romanian): "Rulez `./gradlew testDebugUnitTest`."
> User (turn 2, English): "now run lint"
> Response (English): "Running `./gradlew lintDebug` now."
