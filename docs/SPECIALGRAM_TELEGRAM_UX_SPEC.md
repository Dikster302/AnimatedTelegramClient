# Specialgram — Telegram-style UX and implementation specification

Branch: migration/android-java-native  
Platform: native Android (Java)  
Messaging backend: TDLib

This is the working product and engineering specification for bringing Specialgram closer to Telegram's interaction model. It distinguishes existing behavior from work still required; a target is not a claim that the feature is already implemented.

## Product rules

- Preserve the native Android + TDLib architecture. Do not replace working TDLib flows with mock data.
- Keep the existing application ID, release signing key and in-place update path unchanged.
- Prefer Telegram-like behavior and restrained visual design over decorative effects.
- Every async action must have loading, success and failure behavior. Never silently discard a TDLib error.
- Preserve scroll position, draft text and current selection when refreshing a screen where possible.
- Respect Android system back, keyboard insets, accessibility labels, font scaling and dark/light themes.
- Do not claim that a feature is complete until its TDLib action and error path are wired and the project compiles.

## Main chats screen

### Open the app and load chats
**Current:** TDLib chat state feeds the native chat list; the screen has a title row, search, stories, folder tabs and bottom navigation.

**Target behavior**
1. Keep the top area compact: Specialgram title and overflow menu; the search field is a single full-width row with the magnifier embedded on the left. Do not add a hamburger button.
2. Show the story strip only above the main chat folders, and hide it in Archive.
3. Keep folders horizontally scrollable: All, Personal, Groups, Channels, Archive. The selected folder remains visible after selection.
4. Chat rows show avatar, title, last message, timestamp, delivery/read state where available, unread badge, pin and mute indicators.
5. The floating compose button opens the contact picker/new-message flow. It must stay above the list, respect bottom insets and disappear outside the main chat list and in Archive.
6. Use subtle transitions for screen changes; do not animate the entire chat list on every TDLib update.

**Edge cases**
- Empty account: show a clear first-chat action, not a blank list.
- Empty search/folder: show a contextual empty state and a way to clear the filter.
- Offline: preserve cached chats and drafts; show connection state without replacing usable content.
- Repeated updates: avoid duplicate rows, jumps to the top and unnecessary avatar/story reloads.

### Search and folders
- Search should filter chats as text changes, preserve the query when switching folders, and provide a clear action.
- Folder selection must use TDLib folder/filter semantics rather than merely relabeling the same list.
- Unread-only and sorting controls must visibly reflect their active state.
- If a folder has no chats, show its own empty state.

### Chat row actions
- Tap opens the selected chat.
- Long press opens a contextual menu with pin/unpin, read/unread, mute/unmute and archive/restore.
- The menu labels reflect the current chat state.
- After an action, update the row from TDLib state; if TDLib rejects it, show a readable error and retain the actual server state.
- Avoid duplicate taps while a destructive or state-changing operation is pending.

## Stories

- Show the user's own story entry first, with a camera/add affordance.
- Group other stories by owner and show an unread ring while any story in the group is unviewed.
- Open the first unviewed story; subsequent stories advance in order.
- Viewer supports previous/next, close, reaction and reply only when the corresponding TDLib operation is available.
- Story list/content requests must be deduplicated, cached and rate-limited. Respect TDLib 429 retry_after; do not immediately retry in a loop.
- If media is unavailable, show a retry state and continue to allow closing the viewer.

## Conversation screen

### Open a chat
- Show a compact chat header with back, avatar/name, presence or last-seen state when available, and overflow actions.
- Load recent history first, then older history when the user scrolls upward.
- Preserve the current scroll anchor when new messages arrive unless the user is already at the bottom.
- Show a jump-to-latest affordance when new messages arrive while the user is reading older history.
- Handle deleted chats, inaccessible messages and unavailable media without crashing.

### Send a text message
- Keep the composer anchored above the keyboard/navigation insets.
- Empty text does not send; multiline text expands within a reasonable maximum height.
- Sending immediately inserts a pending bubble; success updates its state, failure keeps the draft/message available for retry.
- Prevent accidental duplicate sends from repeated taps.
- Support retry and clear error feedback when offline or rejected by TDLib.

### Attach media
- Attachment picker must show only supported actions and request Android permissions only when required.
- Show a local preview before sending where the flow supports it.
- Show upload progress and allow cancellation when TDLib supports cancellation.
- On failure, keep the item visible with a retry action; do not silently discard it.
- Large media must not block the UI thread.

### Message interactions
- Long press opens actions supported by the message type: reply, copy, forward, edit (own eligible messages), delete and select.
- Reply preview links to the source message and can be dismissed before sending.
- Forwarding preserves attribution and asks for a destination when required.
- Selection mode shows a clear selected count and exits predictably on Back.
- Respect server-side permissions and message age/edit/delete constraints.

## Profiles and contacts

- Own profile shows current TDLib account data, avatar, username, phone and bio.
- Profile editing validates input locally, sends TDLib mutations and surfaces server errors.
- Photo replacement shows the selected image and upload result.
- Other people's profile opens without adding dialog-owned views to multiple parents.
- Contact search matches display name, username and phone; empty results have a clear state.
- Contact selection from the compose action starts a private-chat creation/open flow.

## Navigation and settings

- Bottom navigation keeps a stable order and active-state indicator.
- Back returns to the previous screen or closes a modal before leaving the app.
- Settings changes are persisted and reflected immediately; failed persistence must not pretend the setting was saved.
- Theme, accent, font size, wallpaper and compact-chat settings must apply consistently to dialogs, menus, lists and message screens.
- Account profile editing remains reachable from both Profile and Settings without creating duplicate screen instances.

## Reliability and release criteria

- No crashes on opening profiles, switching tabs/folders, opening story viewer, rotating/recreating the activity or returning from the media picker.
- No duplicate TDLib requests caused by repeated rendering.
- All network actions expose errors; rate limits use the supplied retry delay.
- Avatars and downloaded media use app-private persistent storage where appropriate and have a cache fallback.
- Compile validation must pass on the current branch after each batch.
- Device testing is still required for keyboard behavior, gestures, animation feel, permissions, media pickers, notifications and in-place APK updates.
- Release APK builds remain manual-only. Never change the release signing key after distribution.
