<p align="center">
  <img src="https://raw.githubusercontent.com/eskiu-lang/eskiu/main/assets/logo.png" alt="Eskiu, a kitten with a ball of yarn" width="140">
</p>

<h2 align="center">pedometer</h2>
<p align="center">Steps walked, live and between two times, for Eskiu ui apps on iOS and Android.</p>
<p align="center"><sub>Part of the <a href="https://github.com/eskiu-lang">Eskiu</a> project</sub></p>

---

```eskiu
import <ui>;
import <pedometer>;

void Walk() {
    Steps now = use_steps();                          // since this mounted, live
    Steps today = use_steps_between(midnight, now_ms); // the app open or not
    if (today.status == StepsReady) { ... today.steps ... }
}
```

| Call | Gives |
|---|---|
| `use_steps()` | the steps since the calling scope mounted, live; on iOS also the `distance` walked in meters |
| `use_steps_between(from_ms, to_ms)` | the steps walked between two times (milliseconds since 1970), asked again when the range changes and each time the app comes to the foreground |

`Steps.status` is `StepsWaiting`, `StepsReady`, `StepsDenied` (the user said no) or
`StepsUnavailable` (no step counter: a simulator, the web, the desktop). The first use asks
for permission: motion on iOS (`NSMotionUsageDescription`, which an app's own `ui.toml` can
word), activity recognition on Android 10 and later.

## Where the steps come from

- **iOS**: CMPedometer, which keeps the device's steps for the last seven days whether the app
  ran or not.
- **Android**: the step counter sensor, which counts from the last boot and keeps no history.
  The package saves a sample of that count whenever it reads it (while `use_steps` or
  `use_steps_between` is mounted, at most once a minute, and each time the app comes to the
  foreground) and answers a range from the samples around it, sharing the steps between two
  samples in proportion to time. So steps walked with the app closed are counted once the app
  is opened again, as long as the device did not reboot in between (after a reboot the count
  starts from 0). Samples are kept for 30 days.

`examples/steps` shows today's steps and this walk's (`ui run ios` or `ui run android`, on a
device).

## Tests

`eskiuc test` runs the hooks against stand-ins for the platform: the live count, the range
query and its answers, permission and availability, the foreground.

## License

MIT
