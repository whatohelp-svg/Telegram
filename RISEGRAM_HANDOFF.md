# RiseGram — project handoff

Last updated: 2026-11-20
Canonical Android repository: https://github.com/whatohelp-svg/Telegram
Web/admin repository: https://github.com/whatohelp-svg/risegram

## Non-negotiable product direction

- This is a full native Telegram Android fork branded RiseGram, not a WebView shell, not a small demo app, and not a replacement client.
- Preserve Telegram's working chats, contacts, calls, groups, channels, settings, media, stickers, gifts, and other native client features while implementing RiseGram branding and its own services.
- Do not replace real account/session data or server-backed features with demo/hard-coded data.
- Do not claim a fix, successful build, deployment, or working login until verified by an actual test/build result.
- Server-only changes should not require an APK rebuild unless native client code/configuration changes.

## Confirmed APK comparison (2026-11-20)

Two user-provided archives were inspected:
- `Risegram-APK.zip`: contains `TMessagesProj_App/build/outputs/apk/afat/debug/app.apk`, APK size 70,215,546 bytes (about 70 MB), SHA-256 `62ec9183cdd210483c576e1fd8c9895094049b114cf12231744911eb8305b6fa`.
- `RiseGram-debug-APK (9).zip`: contains `app.apk`, APK size 126,208,296 bytes (about 126 MB), SHA-256 `f768e4bf2b3a8f10f7a453a51dc60ee4136af108d1d21b188766da36a0d5ec94`.

The APK contents are nearly identical for Java/Dex, assets, and resources. The 70 MB APK contains native libraries only under `lib/arm64-v8a/`. The 126 MB APK contains `libtmessages.49.so` and its companion native libraries for all four ABIs:
- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`

Thus the observed 56 MB size difference is explained by native ABI payloads, not by evidence that Telegram features were removed. The 70 MB APK is ARM64-only. The 126 MB APK is the four-ABI universal APK.

## Build configuration findings

- `TMessagesProj_App/build.gradle` already declares all four ABI filters for the `afat` flavor.
- The intended task is `assembleAfatDebug`.
- The artifact path used by the workflow is `TMessagesProj_App/build/outputs/apk/afat/debug/app.apk`.
- `.github/workflows/build.yml` previously ran `assembleAfatDebug` without cleaning and without checking the APK's ABI contents.
- On 2026-11-20 the workflow was changed in commit `1881d4c93fe50629ea86d5e9d5c8b025a0059bca` to run `clean assembleAfatDebug` and fail unless all four `lib/<abi>/libtmessages.49.so` entries are present.
- Important: the updated GitHub Actions workflow has NOT yet been observed completing successfully. Do not say the corrected APK has been built until its run succeeds and its artifact is inspected.

## Previous technical work to preserve

- Android app repository: `whatohelp-svg/Telegram`.
- RiseGram admin/web repository: `whatohelp-svg/risegram`.
- Android network work in recent commits includes RiseGram hostname resolution before TCP socket creation and a pinned static DC port without requiring a proxy.
- Do not overwrite the RiseGram DC/network configuration with stock Telegram DC routes.
- A prior working direction used the native MTProto Telegram client with RiseGram infrastructure. Persistent login/session and network connectivity have been recurring problems and require real-device validation.
- Calls/video may require correctly configured TURN/SFU infrastructure; do not claim they work solely because the UI exists.

## RiseGram product/economy requirements

- Keep the full Telegram-native client behavior.
- RiseGram UI and bottom navigation should follow the user's supplied visual references.
- Restore/use the actual Telegram gifts catalog and gift sending flow, not a manually typed fake inventory.
- The user requested all Telegram NFT gifts in the drop/catalog, sourced from Telegram's published gifts data/site where possible.
- Gram Earrings is a RiseGram cryptocurrency, not a gift label: 1 Gram = 100 RiseStars.
- Preserve Gram wallet/exchange functionality; do not silently remove it.
- New users should receive a random Kazakhstan-style `+7 (986)...` number in the RiseGram account system. Do not assign the scarce `+888` number to everyone.
- Premium, Stars, RiseStars, inventory, and drop mechanics must use real server-backed state rather than hard-coded demo balances/items.

## Known web/admin regression to investigate separately

In `whatohelp-svg/risegram`, commit `f9867610698111a4bca4486311f2a84e70e0d84e` (2026-11-20) modified `index.html` and restored a legacy/demo interface. The inspected diff indicated that it removed/replaced real auth/session UI, Gram Earrings section, and Telegram gift catalog retrieval with hard-coded demo profile data and manual gift entry. This is a web/admin repository regression, not the cause of the Android APK ABI size difference. Re-check current branch before editing; do not assume this commit is still HEAD.

## Immediate next steps

1. Check GitHub Actions for commit `1881d4c93fe50629ea86d5e9d5c8b025a0059bca`. If the workflow fails, inspect the first missing ABI/build error and fix the native build output path or ABI build process. Do not remove ABI filters.
2. Download the resulting artifact only after a successful workflow run. Inspect the APK and verify all four `lib/<abi>/libtmessages.49.so` entries before sending it to the user.
3. Keep the Android app as the canonical full client; separately repair the web/admin regression in `whatohelp-svg/risegram`.
4. Resume the real Telegram gift catalog/drop integration and validate gift sending against backend state.
5. Continue MTProto login/session/network troubleshooting with actual logs and device tests.

## Handoff protocol for every future chat

At the start of a continuation:
1. Read this file first.
2. Check the latest commit on `master` in both repositories; this file is a handoff, not proof that no newer changes exist.
3. Check current GitHub Actions status and the latest APK artifact before describing build status.
4. Update this file in the same commit or a follow-up commit whenever a significant decision, fix, failure, artifact, or next step changes.
5. In the chat, tell the user the exact repository, commit SHA, files changed, build/test status, and what remains. Never imply that work was performed if only a plan was made.
