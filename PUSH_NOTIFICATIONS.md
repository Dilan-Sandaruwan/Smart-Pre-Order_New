# Firebase push notifications

Push delivery uses Firebase Cloud Messaging only. No Supabase notification tables,
RPCs, Edge Functions, or database changes are required. The rest of the app still
uses its existing Supabase authentication and order storage.

## Configuration

The Firebase Android configuration is `app/google-services.json`. The package name
must be `com.smartpreorder.app`. Build and install the app, open it, and allow Android
notification permission. Use a phone or emulator with Google Play services.

## Send a notification

1. Open your Firebase project and go to **Messaging**.
2. Create a Firebase notification campaign and enter its title and body.
3. Select this Android application as the target, or select a subscribed topic.
4. Review and publish the campaign.
5. Test with the app both visible and in the background. Tap the notification to
   open the app.

After sign-in the app automatically subscribes to `all_users` and its current role:

| Topic | Audience |
| --- | --- |
| `all_users` | All signed-in accounts |
| `admin` | Signed-in admins |
| `customer` | Signed-in customers |
| `restaurant` | Signed-in restaurant owners |

Topics become available after a device successfully subscribes and Firebase has
processed the subscription. Subscriptions are updated after account changes and
cleared asynchronously at logout. Token changes and registration failures retry
without a Supabase notification call. Firebase Console can also target the Android
application directly for a general announcement, including signed-out installations.

These topics are for public announcements: FCM client subscriptions are not an
access-control mechanism. Do not use them for private account or order details.
Console notification messages are displayed automatically by Firebase in the
background; the app displays them itself in the foreground. Data messages with
`title` and `body` are also supported. Optional `user_id` and `role` data fields are
filtered locally when the app handles a data message; they do not secure topics
or filter automatic background notification messages.

## Order updates

The app retains its existing local order alerts while its realtime order listener
is running. Firebase Console announcements do not automatically follow order
changes. Automatic order pushes require a trusted sender such as a Firebase Cloud
Function connected to order events; this Firebase-only client does not deploy one.

Notification permission, disabled channels, battery restrictions, connectivity,
and Google Play services affect delivery. Android force-stop prevents delivery
until the app is reopened. Notifications open the normal role home screen.

## Build

From a terminal with Java and the Android SDK available:

```sh
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Live delivery must be tested by publishing a campaign from your Firebase project.

References: [Receive Android messages](https://firebase.google.com/docs/cloud-messaging/android/receive-messages),
[Topic subscriptions](https://firebase.google.com/docs/cloud-messaging/manage-topic-subscriptions).
