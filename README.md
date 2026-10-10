# SmartPreOrder – Android app (Kotlin + Jetpack Compose + Supabase)

Restaurant pre-ordering and real-time order tracking, built from the
SmartPreOrder project proposal (NIBM Kurunegala – HNDSE Mobile App Development).

One app, three roles (chosen at registration / set by the admin):

| Role | What they can do |
|------|------------------|
| **Customer** | Register / log in, search restaurants and dishes, browse menus by category, cart, place **Dine-in** or **Pickup** orders, live order tracking with estimated time, notifications, order history, ratings & reviews, report a restaurant |
| **Restaurant** | Create restaurant profile, live order queue, accept (with ETA) / reject (with reason), Start preparing -> Ready -> Completed, change ETA, manage menu (add / edit / delete / sold-out toggle), open/closed switch, sales summary + order history |
| **Admin** | Dashboard statistics, activate/deactivate restaurants, block/unblock users, monitor all orders, manage food categories, handle reported content |

## 1. Set up Supabase (5 minutes)

1. Create a project at <https://supabase.com>.
2. **SQL Editor -> New query**, paste all of `supabase/schema.sql`, press **Run**.
   (Creates tables, security rules, order functions, realtime and seed categories.)
3. **Authentication -> Providers -> Email**: for testing, turn **off** "Confirm email"
   (otherwise users must click a link in their inbox before they can log in).
4. **Project Settings -> API**: copy the *Project URL* and the *anon / publishable key*.

## 2. Run the app

1. Install the latest **Android Studio**, then **File -> Open** and pick this folder.
2. Copy `local.properties.example` to `local.properties` and fill in `SUPABASE_URL` and
   `SUPABASE_ANON_KEY` (Android Studio keeps `sdk.dir` in the same file – don't delete it).
3. Let Gradle sync (needs internet the first time) and press **Run** on an emulator / phone
   (Android 8.0+ / API 26+).

## 3. Create the admin account

Register once in the app (any role), then in the Supabase SQL Editor run:

```sql
update public.profiles set role = 'admin'
where id = (select id from auth.users where email = 'your-email@example.com');
```

Log out and log in again – you'll see the admin screens.

## 4. Try the full flow (two devices/emulators, or one after the other)

1. Register a **restaurant owner**, create the restaurant, add menu items.
2. Register a **customer** on another device, open the restaurant, order (Pickup / Dine-in).
3. On the restaurant device the order pops up instantly -> Accept with ETA -> Start preparing -> Mark ready.
4. The customer's screen updates live and a notification is shown at each step.
5. Mark completed -> the customer can rate the meal.

## Project structure

```
app/src/main/java/com/smartpreorder/app/
  data/            Models.kt  Repo.kt (all Supabase calls)  SupabaseProvider.kt
  notifications/   Notifier.kt  (local notifications)
  ui/              Root.kt (role router + login)  ViewModels.kt
                   CustomerScreens.kt  RestaurantScreens.kt  AdminScreens.kt
                   Components.kt  Theme.kt  Icons.kt
supabase/schema.sql   database, row level security, functions
```

Architecture: Compose UI -> ViewModels (state) -> `Repo` -> Supabase (Auth + Postgres via REST + Realtime).

## How it maps to the proposal

| Proposal | In this project |
|---|---|
| Native Android (Kotlin) | Kotlin + Jetpack Compose |
| Node.js + Express + MySQL/PostgreSQL | **Supabase Postgres** (REST via supabase-kt) + SQL functions (`place_order`, `cancel_order`, `admin_stats`) |
| Firebase Authentication | **Supabase Auth** (e-mail / password) |
| Firebase / WebSocket real-time | **Supabase Realtime** (Postgres changes on `orders`) |
| Firebase Cloud Messaging | Local notifications from realtime events (see limitations) |
| Security | Row Level Security on every table; prices/totals calculated in the database |

## Known limitations / next steps

* **Notifications work while the app process is alive.** For alerts when the app is fully
  closed, add Firebase Cloud Messaging + a Supabase Edge Function (listed under future work).
* No image upload yet (`image_url` columns exist; Supabase Storage can be added).
* No online payment (the proposal lists it as future work) – customers pay at the restaurant.
* Google Maps SDK is not included; the address is shown as text.
* The Gradle wrapper JAR is not bundled – Android Studio downloads Gradle automatically on sync.
* If Gradle complains about a Ktor version mismatch, set `ktor-client-okhttp` in
  `app/build.gradle.kts` to the Ktor version listed in the supabase-kt release notes.
