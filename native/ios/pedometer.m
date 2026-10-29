// <pedometer> on iOS: CMPedometer. Live counts and query answers go to the "pedometer" channel
// (src/pedometer.esk). The first use asks the user for motion permission.
#import <CoreMotion/CoreMotion.h>
#import "ui_native.h"

static CMPedometer *pedometer;

static CMPedometer *shared(void) {
    if (!pedometer) pedometer = [CMPedometer new];
    return pedometer;
}

static BOOL denied(void) {
    CMAuthorizationStatus status = CMPedometer.authorizationStatus;
    return status == CMAuthorizationStatusDenied || status == CMAuthorizationStatusRestricted;
}

static BOOL deniedError(NSError *error) {
    return error.domain == CMErrorDomain && (error.code == CMErrorMotionActivityNotAuthorized ||
                                             error.code == CMErrorMotionActivityNotEntitled);
}

void pedometer_start(void) {
    if (!CMPedometer.isStepCountingAvailable) { ui_native_emit("pedometer", "unavailable"); return; }
    if (denied()) { ui_native_emit("pedometer", "denied"); return; }
    [shared() startPedometerUpdatesFromDate:[NSDate date] withHandler:^(CMPedometerData *data, NSError *error) {
        if (error) { ui_native_emit("pedometer", deniedError(error) ? "denied" : "unavailable"); return; }
        char payload[96];
        snprintf(payload, sizeof payload, "steps %lld %.2f", data.numberOfSteps.longLongValue,
                 data.distance ? data.distance.doubleValue : -1.0);
        ui_native_emit("pedometer", payload);
    }];
    // The first update comes with the first step: say 0 now.
    ui_native_emit("pedometer", "steps 0 0");
}

void pedometer_stop(void) { [shared() stopPedometerUpdates]; }

void pedometer_query(int id, double from_ms, double to_ms) {
    char payload[96];
    if (!CMPedometer.isStepCountingAvailable) {
        snprintf(payload, sizeof payload, "query %d unavailable", id);
        ui_native_emit("pedometer", payload);
        return;
    }
    NSDate *from = [NSDate dateWithTimeIntervalSince1970:from_ms / 1000.0];
    NSDate *to = [NSDate dateWithTimeIntervalSince1970:to_ms / 1000.0];
    [shared() queryPedometerDataFromDate:from toDate:to withHandler:^(CMPedometerData *data, NSError *error) {
        char answer[96];
        if (error) snprintf(answer, sizeof answer, "query %d %s", id, deniedError(error) ? "denied" : "unavailable");
        else snprintf(answer, sizeof answer, "query %d %lld", id, data.numberOfSteps.longLongValue);
        ui_native_emit("pedometer", answer);
    }];
}

void pedometer_sample(void) {}
