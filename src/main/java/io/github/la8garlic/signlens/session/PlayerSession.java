package io.github.la8garlic.signlens.session;

import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import io.github.la8garlic.signlens.render.RenderDecision;
import io.github.la8garlic.signlens.render.RenderPolicy;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.time.Instant;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Per-player state holder; behavior remains in the focus and render controllers. */
public final class PlayerSession {

    private final UUID playerId;
    private final FocusController focusController;
    private final RenderPolicy renderPolicy;
    private ViewSample lastView;
    private SignSnapshot lastSnapshot;
    private ScheduledTask scanTask;
    private Instant lastRayTraceAt;
    private long lastRayTraceNanos;
    private OptionalDouble lastDistance = OptionalDouble.empty();

    public PlayerSession(UUID playerId) {
        this(playerId, new FocusController(), new RenderPolicy());
    }

    public PlayerSession(UUID playerId, FocusController focusController, RenderPolicy renderPolicy) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.focusController = Objects.requireNonNull(focusController, "focusController");
        this.renderPolicy = Objects.requireNonNull(renderPolicy, "renderPolicy");
    }

    public UUID playerId() {
        return playerId;
    }

    public FocusController focusController() {
        return focusController;
    }

    public RenderPolicy renderPolicy() {
        return renderPolicy;
    }

    public Optional<ViewSample> lastView() {
        return Optional.ofNullable(lastView);
    }

    public void lastView(ViewSample view) {
        lastView = Objects.requireNonNull(view, "view");
    }

    public void clearLastView() {
        lastView = null;
    }

    public Optional<SignSnapshot> lastSnapshot() {
        return Optional.ofNullable(lastSnapshot);
    }

    public void lastSnapshot(SignSnapshot snapshot) {
        lastSnapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public void clearLastSnapshot() {
        lastSnapshot = null;
    }

    public Optional<Instant> lastRayTraceAt() {
        return Optional.ofNullable(lastRayTraceAt);
    }

    public long lastRayTraceNanos() {
        return lastRayTraceNanos;
    }

    public OptionalDouble lastDistance() {
        return lastDistance;
    }

    public void recordRayTrace(Instant at, long durationNanos, OptionalDouble distance) {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(distance, "distance");
        if (durationNanos < 0) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
        lastRayTraceAt = at;
        lastRayTraceNanos = durationNanos;
        lastDistance = distance;
    }

    public Optional<ScheduledTask> scanTask() {
        return Optional.ofNullable(scanTask);
    }

    public void scanTask(ScheduledTask task) {
        Objects.requireNonNull(task, "task");
        if (scanTask != null && scanTask != task) {
            scanTask.cancel();
        }
        scanTask = task;
    }

    public void retireTask() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    public void taskRetired() {
        scanTask = null;
    }

    /** Resets all view/focus/render state after teleport, world change, or reload. */
    public RenderDecision reset() {
        focusController.reset();
        clearLastView();
        clearLastSnapshot();
        lastRayTraceAt = null;
        lastRayTraceNanos = 0L;
        lastDistance = OptionalDouble.empty();
        return renderPolicy.reset();
    }
}
