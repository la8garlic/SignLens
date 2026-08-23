package io.github.la8garlic.signlens.session;

import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import io.github.la8garlic.signlens.render.RenderDecision;
import io.github.la8garlic.signlens.render.RenderPolicy;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Per-player state holder; behavior remains in the focus and render controllers. */
public final class PlayerSession {

    private final UUID playerId;
    private final FocusController focusController;
    private final RenderPolicy renderPolicy;
    private ViewSample lastView;
    private SignSnapshot lastSnapshot;
    private ScheduledTask scanTask;

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
        return renderPolicy.reset();
    }
}
