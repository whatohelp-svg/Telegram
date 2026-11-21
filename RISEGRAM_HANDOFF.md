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

## Latest user-reported UI regressions (2026-11-20)

- The user reports that the Gifts tab is still missing and Gram Earnings is absent.
- The Gram Earnings settings row was restored on Android master in commit `fd5d6c405d17e60cc7c29edb1c414e53c76e78be` by restoring the existing `StarsController.getTonInstance` entry (setting item id 13). The existing click handler for id 13 is present. This source change requires a new APK and has not yet been built or device-tested.
- The user screenshot of group settings shows the public username field containing a Railway MTProto hostname (`risegram-mtproto-risegram.up.railway...`) rather than a valid group username. Investigate the public link prefix / username initialization and ensure Railway service hostnames are never used as Telegram public usernames. Do not assume the user typed it.
- Telegram profile gifts are implemented in `ProfileActivity` and the profile media tab uses `SharedMediaLayout.TAB_GIFTS`; verify why the user sees no Gifts tab. The standard profile tab may be conditional on a non-zero gifts count, so distinguish an absent profile tab from a missing Settings / bottom-navigation entry. Existing SettingsActivity includes a `SendAGift` row.
- Do not claim these regressions are fully fixed until a fresh APK is built and checked on the user's device.

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


## Follow-up status (2026-11-20 22:00 +05)

- Latest master commit before this continuation was `eb7bcc3513e80a5a7e821ef4abef4c927aedbed6` ("Restore native RiseGram wallet, gifts and verified profile marks").
- GitHub Actions run #48 (`44772193837`) failed after 31m54s at Java compilation. Exact error: `SharedMediaLayout.java:6661: cannot find symbol: variable currentAccount`.
- Fixed the compile error in commit `becdd99c2af27501d7c77491b6d3f72b573ef49f` by retrieving the selected account from the owning `profileActivity` when checking whether the profile belongs to the current user. This preserves the intended own-profile Gifts tab behavior without referencing a nonexistent field.
- A new Actions run should be triggered by this commit; check its actual result before claiming the APK is built.
- Remaining investigation: the group public-link prefix is populated from `MessagesController.linkPrefix`; verify the value supplied by the MTProto config and ensure it is a public Telegram-style domain (e.g. `t.me`), never the Railway MTProto service hostname. Do not change the actual public username field or silently rewrite existing group usernames.
- The verified-profile change currently relies on the MTProto `user.verified` flag for the blue check and adds the blue Telegram plane as a left-side icon. Confirm the RiseGram MTProto server sets that flag only for RiseGram-approved accounts; client-side hard-coded verification is not acceptable.
- Gifts: commit `eb7bcc3` makes the native profile Gifts tab visible for the user's own profile even at zero gift count and restores the native Settings `Send a Gift` entry. Confirm that the native catalog and send flow are available in the running MTProto backend; the Android source already contains Telegram's native `Gifts/SendGiftSheet` and related screens.
- Gram Earnings: commit `eb7bcc3` adds a separate wallet screen using RiseGram REST endpoints and persists its wallet token in SharedPreferences. This wallet token is separate from the Telegram MTProto login session; do not conflate the two or clear Telegram auth data while debugging it.
- Persistent Telegram login still needs device-level validation after app process restart. Preserve UserConfig/session files and never reset the local database as a debugging shortcut.


## Continuation update (2026-11-20 22:05 +05)

- Fixed the compile error in `SharedMediaLayout.java` in commit `becdd99c2af27501d7c77491b6d3f72b573ef49f`; the code now resolves the selected account through `profileActivity.getCurrentAccount()`.
- Corrected the Settings `Send a Gift` action in commit `b93851a9df5f670debf53eea4a47449f1718b115`: it now opens `UserSelectorBottomSheet.TYPE_STAR_GIFT`, which routes into the native Telegram Star Gifts catalog/send flow rather than the default Premium gift-code picker. The existing native gifts screens are retained.
- Investigated the Railway public-link prefix in the MTProto submodule `iamxvbaba/gramsrv`: `help.getConfig` builds `me_url_prefix` from `TELESRV_PUBLIC_BASE_URL` (the value is not the MTProto DC endpoint). Set that variable on Railway project `acceptable-gratitude`, environment `risegram`, service `risegram-mtproto` to `https://t.me`; this leaves `TELESRV_ADVERTISE_HOST/PORT` and the actual MTProto TCP proxy unchanged. Railway deployment `c0ae15d6-e204-4a15-8ab2-f1e0b4d95924` was `DEPLOYING` at the time of this note; verify it reaches `SUCCESS` and inspect the returned `help.getConfig`/client display before claiming the prefix fix is live.
- Android build run #48 failed due to the missing `currentAccount` symbol. Runs #49 and #50 were cancelled by the newer workflow concurrency group. Run #51 (`44779527596`, commit `b93851a9df5f670debf53eea4a47449f1718b115`) was in progress; verify its terminal status and inspect the four ABI entries in the artifact.
- Railway MTProto logs still show `AUTH_KEY_UNREGISTERED` for some previously seen raw auth keys after service restart. The app's `ConnectionsManager.onLogout` clears `UserConfig` only when the server explicitly reports logout / unregistered key; do not remove that safety behavior or reset the DB. The MTProto service has its state volume mounted at `/var/lib/telesrv` and Postgres/Redis are online. Investigate persistence/binding behavior in the upstream `iamxvbaba/gramsrv` submodule; this fork does not have write permission to that upstream repo.
- User-reported relogin after app restart remains unverified on a physical device. A backend log contains both successful `auth.signIn` and later `AUTH_KEY_UNREGISTERED` for some auth keys; do not state that persistence is fixed without device testing.
- Verification UI: the Android change uses the server-provided MTProto `user.verified` flag for the blue verified check and adds the blue Telegram plane at the left. Confirm server-side verification authorization before describing it as RiseGram-specific verification; no hard-coded user ID or client-only fake check.

## Continuation update (2026-11-21 10:26 +05)

### Android client changes on master

- 618e719b2245d2be2cf0672f5862b6df15ab6031 changed the Settings label from "Gram Earnings" to "Gram".
- a9d9ddaabd0594670473554e76964e8af97c638e added the Russian "Gram" label; 0a4fd517c6860c522c145a4f6aa6f53d89c63464 added the RiseGram blue verification badge drawable.
- 26bc021b8893dbc1806bf42cfcae41fd79692f84 clarified the Gram screen as access to the RiseGram currency balance, not a second wallet.
- e809eea021de0cf3cd19cb7ba21c98c39161c069 separated the RiseGram verification text from Major.
- 9a224477170a64fa2483b0845675798f5a823468 made the profile footer conditional on server verification and now shows the RiseGram verification text/icon for ordinary verified accounts, or Major text/icon when the public username is @major.
- Gram remains backed by the RiseGram economy API and retains its own RiseGram account session token. Do not remove that authorization or reuse Telegram UserConfig: the RiseGram economy database still has a separate account identity. The user-facing section is labelled "Gram" and shows the 1 Gram = 100 RiseStars conversion.
- The Android client changes are committed, but this continuation has not yet completed a fresh APK build from the Telegram repository or a device install/relaunch test. The handoff update commit should trigger the master APK workflow; verify its artifact and all four native ABI libraries before release.

### NFT gift drop implementation on feature/market-economy-sync

- Added database.py support for upgrade_stars and pay_for_upgrade columns via the existing SQLite schema migration. Catalog synchronization adds missing bot-sendable gifts which can be upgraded to unique gifts, keeps live drops idempotent, and enforces a base-gift-plus-upgrade RiseStars price floor.
- Added the shared gift_catalog.py module so the bot, admin panel and HTTP API can read Telegram's live gift catalog without circular imports.
- The admin drop catalog now synchronizes the bot's available NFT-upgrade gifts and supports manually setting limited stock and a RiseStars price that cannot undercut the Telegram gift+upgrade cost.
- Added a player-facing "Подарки" entry to the bot main menu, /gifts, paging, a "Мои подарки" inventory, and purchase callbacks. The purchase path atomically reserves one unit and RiseStars, sends the Telegram gift with pay_for_upgrade=True, then confirms the inventory row.
- Explicit Telegram API rejections refund the RiseStars and restore stock. If the network result is ambiguous, the reservation remains pending to avoid sending a duplicate; pending items are now visible in the player's gift inventory and API response.
- Automated tests in tests/test_gift_catalog.py cover idempotent catalog sync, NFT upgrade pricing, limited stock, purchase rollback/confirmation and pending inventory display.
- Scope limitation: the catalog can include every NFT-upgrade-capable gift returned by Bot API getAvailableGifts for this bot. It cannot automatically list every already-minted collectible owned by other users or items outside the bot's sendable gift catalog. Do not describe this as a complete marketplace-wide NFT transfer feature.

### Backend deployment and login status

- The RiseGram bot/API service successfully started on Railway at commit 86aaeb8bf632decd19eee9d92c79b49d9409325b; logs showed "HTTP API listening", "RiseGram запущен как @risegrambot", and polling started. After the pending-inventory commits, the latest observed bot deployment for 6bcccab42bceac5847bd3e2fa62d5a56e045cdf8 was SUCCESS, and a new container again reported the API and bot polling started.
- The MTProto service's deployment at commit 86aaeb8bf632decd19eee9d92c79b49d9409325b was SUCCESS. Later commits also queued/restarted this service; verify its newest deployment is SUCCESS before release.
- The previous full backend validation run 44870618404 completed successfully on commit 86aaeb8bf632decd19eee9d92c79b49d9409325b, including the gift-catalog tests. The pending-inventory test update after that run still needs a fresh validation result.
- Recent logs checked after 2026-11-21 05:23:19Z did not show a new AUTH_KEY_UNREGISTERED. Older logs included PHONE_CODE_INVALID / PHONE_CODE_EXPIRED attempts followed by successful auth.signIn authorization committed for the same permanent auth key. This does not prove the user's cold-relaunch case is fixed.
- Preserve ConnectionsManager.onLogout, UserConfig, the local session database, the persistent MTProto RSA volume and server auth gates. Never hide AUTH_KEY_UNREGISTERED or reset the database as a workaround. Only call persistent login fixed after the user verifies a full app close/reopen and fresh authenticated RPCs on a real device.

### Release gate

1. Verify the newest RiseGram bot and MTProto Railway deployments are both SUCCESS; inspect startup and auth logs for the latest restart.
2. Re-run the backend suite after the pending-inventory changes and retain the full successful result.
3. Check the Telegram repository master APK workflow triggered by this handoff update; download the artifact, confirm it is a real APK and inspect lib/{armeabi-v7a,arm64-v8a,x86,x86_64}/libtmessages.49.so.
4. On a real device, close RiseGram completely, relaunch, confirm the account remains signed in, open Gram, open the gifts menu, purchase one NFT-upgrade gift on a test account and verify the user's Telegram gift plus RiseGram inventory, stock decrement and RiseStars debit.
5. Do not declare the private release ready until the above device checks are completed.
