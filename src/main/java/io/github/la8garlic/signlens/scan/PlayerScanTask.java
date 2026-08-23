package io.github.la8garlic.signlens.scan;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.detection.SignDetector;
import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.focus.FocusTarget;
import io.github.la8garlic.signlens.focus.FocusTransition;
import io.github.la8garlic.signlens.focus.FocusState;
import io.github.la8garlic.signlens.reading.SignReader;
import io.github.la8garlic.signlens.reading.SignSnapshot;
import io.github.la8garlic.signlens.render.ContentFormatter;
import io.github.la8garlic.signlens.render.FormattedContent;
import io.github.la8garlic.signlens.render.RenderDecision;
import io.github.la8garlic.signlens.render.RenderDecisionType;
import io.github.la8garlic.signlens.render.SignRenderer;
import io.github.la8garlic.signlens.session.PlayerSession;
import io.github.la8garlic.signlens.session.ViewSample;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Entity-owned scan loop that invokes detection, focus, reading, and rendering in order. */
public final class PlayerScanTask {

    public static final String USE_PERMISSION = "signlens.use";

    private final Plugin plugin;
    private final Player player;
    private final PlayerSession session;
    private final SignDetector detector;
    private final SignReader reader;
    private final ContentFormatter formatter;
    private final SignRenderer renderer;
    private final ScanSettings settings;
    private final BooleanSupplier enabled;
    private final Clock clock;
    private final ViewChangeDetector viewChanges;
    private boolean stopped;

    public PlayerScanTask(
            Plugin plugin,
            Player player,
            PlayerSession session,
            SignDetector detector,
            SignReader reader,
            ContentFormatter formatter,
            SignRenderer renderer,
            ScanSettings settings,
            BooleanSupplier enabled
    ) {
        this(plugin, player, session, detector, reader, formatter, renderer, settings, enabled, Clock.systemUTC());
    }

    public PlayerScanTask(
            Plugin plugin,
            Player player,
            PlayerSession session,
            SignDetector detector,
            SignReader reader,
            ContentFormatter formatter,
            SignRenderer renderer,
            ScanSettings settings,
            BooleanSupplier enabled,
            Clock clock
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.viewChanges = new ViewChangeDetector(
                settings.positionThreshold(),
                settings.rotationThresholdDegrees(),
                settings.idleProbeTicks()
        );
    }

    /** Starts exactly one fixed-rate task on this player's entity scheduler. */
    public boolean start() {
        if (stopped || session.scanTask().isPresent()) {
            return false;
        }
        stopped = false;
        ScheduledTask task = player.getScheduler().runAtFixedRate(
                plugin,
                ignored -> runScan(),
                session::taskRetired,
                1L,
                settings.scanPeriodTicks()
        );
        if (task == null) {
            return false;
        }
        session.scanTask(task);
        return true;
    }

    public void stop() {
        stopped = true;
        session.retireTask();
    }

    /** One scheduled scan cycle; package-visible for deterministic runtime tests. */
    void runScan() {
        if (stopped || !plugin.isEnabled()) {
            return;
        }
        if (!enabled.getAsBoolean() || !player.isOnline() || !player.hasPermission(USE_PERMISSION)) {
            apply(session.reset());
            return;
        }

        ViewSample currentView = ViewSample.from(player);
        if (session.lastView().isEmpty()) {
            viewChanges.reset();
        }
        if (!viewChanges.shouldTrace(currentView, settings.scanPeriodTicks())) {
            return;
        }
        viewChanges.recordTrace(currentView);
        session.lastView(currentView);

        Instant now = clock.instant();
        Optional<DetectedSign> detected = detector.detect(player);
        FocusController focus = session.focusController();
        FocusTransition transition = focus.observe(
                detected.map(PlayerScanTask::focusTarget),
                now
        );

        if (transition.focusEnded()) {
            apply(session.renderPolicy().observe(Optional.empty(), false, now));
        }
        if (transition.currentState() != FocusState.FOCUSED || detected.isEmpty()) {
            return;
        }

        Optional<SignSnapshot> snapshot = reader.read(player, detected.orElseThrow());
        if (snapshot.isEmpty()) {
            session.clearLastSnapshot();
            apply(session.renderPolicy().observe(Optional.empty(), true, now));
            return;
        }

        SignSnapshot value = snapshot.orElseThrow();
        session.lastSnapshot(value);
        Optional<FormattedContent> formatted = formatter.format(value);
        apply(session.renderPolicy().observe(formatted, true, now));
    }

    private void apply(RenderDecision decision) {
        if (decision.type() == RenderDecisionType.SHOW) {
            renderer.show(player, decision.content().orElseThrow());
        } else if (decision.type() == RenderDecisionType.CLEAR) {
            renderer.clear(player);
        }
    }

    private static FocusTarget focusTarget(DetectedSign detected) {
        return new FocusTarget(detected.worldId(), detected.x(), detected.y(), detected.z());
    }
}
