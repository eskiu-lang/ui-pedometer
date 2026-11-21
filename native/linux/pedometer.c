// <pedometer> on Linux: computers have no step counter, so every use is unavailable (StepsUnavailable),
// as on the web. The app builds and links; it gets no steps, never made-up ones.
#include <stdio.h>
#include "ui_native.h"

void pedometer_start(void) { ui_native_emit("pedometer", "unavailable"); }

void pedometer_stop(void) {}

void pedometer_query(int id, double from_ms, double to_ms) {
    (void)from_ms;
    (void)to_ms;
    char payload[48];
    snprintf(payload, sizeof payload, "query %d unavailable", id);
    ui_native_emit("pedometer", payload);
}

void pedometer_sample(void) {}
