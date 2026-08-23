package io.github.la8garlic.signlens.scan;

import io.github.la8garlic.signlens.detection.DetectedSign;
import io.github.la8garlic.signlens.detection.SignDetector;
import io.github.la8garlic.signlens.focus.FocusController;
import io.github.la8garlic.signlens.focus.FocusTarget;
import io.github.la8garlic.signlens.focus.FocusTransition;
import io.github.la8garlic.signlens.focus.FocusState;
import io.github.la8garlic.signlens.metrics.PerformanceCounters;
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
import java.util.OptionalDouble;
import java.util.function.BooleanSupplier;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
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
    private final PerformanceCounters counters;
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
        this(
                plugin,
                player,
                session,
                detector,
                reader,
                formatter,
                renderer,
                settings,
                enabled,
                new PerformanceCounters(),
                Clock.systemUTC()
        );
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
        this(
                plugin,
                player,
                session,
                detector,
                reader,
                formatter,
                renderer,
                settings,
                enabled,
                new PerformanceCounters(),
                clock
        );
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
            PerformanceCounters counters
    ) {
        this(plugin, player, session, detector, reader, formatter, renderer, settings, enabled, counters, Clock.systemUTC());
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
            PerformanceCounters counters,
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
        this.counters = Objects.requireNonNull(counters, "counters");
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
        long scanStarted = System.nanoTime();
        try {
            if (!enabled.getAsBoolean() || !player.isOnline() || !player.hasPermission(USE_PERMISSION)) {
                apply(session.reset());
                return;
            }

            ViewSample currentView = ViewSample.from(player);
            if (session.lastView().isEmpty()) {
                viewChanges.reset();
            }
            if (!viewChanges.shouldTrace(currentView, settings.scanPeriodTicks())) {
                counters.recordScanSkip();
                return;
            }
            if (viewChanges.lastDecisionWasIdleProbe()) {
                counters.recordIdleProbe();
            }
            viewChanges.recordTrace(currentView);
            session.lastView(currentView);

            Instant now = clock.instant();
            long traceStarted = System.nanoTime();
            Optional<DetectedSign> detected = detector.detect(player);
            long traceDuration = System.nanoTime() - traceStarted;
            counters.recordRayTrace(detected.isPresent(), traceDuration);
            session.recordRayTrace(now, traceDuration, detected.map(value -> distance(player, value))
                    .orElseGet(OptionalDouble::empty));
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
            counters.recordSnapshotRead();
            if (snapshot.isEmpty()) {
                session.clearLastSnapshot();
                apply(session.renderPolicy().observe(Optional.empty(), true, now));
                return;
            }

            SignSnapshot value = snapshot.orElseThrow();
            session.lastSnapshot(value);
            counters.recordFormatterInvocation();
            Optional<FormattedContent> formatted = formatter.format(value);
            apply(session.renderPolicy().observe(formatted, true, now));
        } finally {
            counters.recordScan(System.nanoTime() - scanStarted);
        }
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

    private static OptionalDouble distance(Player player, DetectedSign detected) {
        Location origin = player.getLocation();
        Location target = new Location(
                player.getWorld(),
                detected.x() + 0.5,
                detected.y() + 0.5,
                detected.z() + 0.5
        );
        try {
            return OptionalDouble.of(origin.distance(target));
        } catch (IllegalArgumentException ignored) {
            return OptionalDouble.empty();
        }
    }
}
