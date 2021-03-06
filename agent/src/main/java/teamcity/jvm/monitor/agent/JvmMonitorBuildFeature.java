package teamcity.jvm.monitor.agent;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collection;

public class JvmMonitorBuildFeature extends AgentLifeCycleAdapter {

    private static Logger log = Logger.getLogger("jetbrains.buildServer.AGENT");

    private final ArtifactsWatcher artifactsWatcher;

    private JvmMonitorLauncher monitor;

    private File outputDir;

    public JvmMonitorBuildFeature(ArtifactsWatcher artifactsWatcher, EventDispatcher<AgentLifeCycleAdapter> eventDispatcher) {
        this.artifactsWatcher = artifactsWatcher;
        eventDispatcher.addListener(this);
    }

    @Override
    public void buildStarted(AgentRunningBuild build) {
        Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType("jvm-monitor-plugin");
        if (!features.isEmpty()) {
            log.info("jvm-monitor-plugin feature enabled for build");

            BuildAgentConfiguration config = build.getAgentConfiguration();
            outputDir = new File(config.getTempDirectory(), "jvmmon");
            boolean result = outputDir.mkdirs();
            if (!result) {
                log.warn("Failed to create output directory");
            }
            monitor = new JvmMonitorLauncher(outputDir.getAbsolutePath());
            try {
                monitor.start();
                log.info("Started JVM Monitor for build");
            }
            catch (Exception e) {
                log.warn("Start monitor failed", e);
            }
        }
    }

    @Override
    public void beforeBuildFinish(AgentRunningBuild build, BuildFinishedStatus buildStatus) {
        if (monitor != null) {
            try {
                monitor.stop();
            } catch (Exception e) {
                log.warn("Stop monitor failed", e);
            }
            monitor = null;
            log.info("Stopped JVM Monitor");
            artifactsWatcher.addNewArtifactsPath(outputDir.getAbsolutePath() + "=>" + ".teamcity/jvmmon/");
        }
    }
}
