package javaposse.jobdsl.dsl.helpers

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobType
import javaposse.jobdsl.dsl.WithXmlAction
import javaposse.jobdsl.dsl.helpers.common.MavenContext
import javaposse.jobdsl.dsl.helpers.step.AbstractStepContext

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Preconditions.checkState

class MavenHelper extends AbstractHelper implements MavenContext {
    JobManagement jobManagement
    StringBuilder allGoals = new StringBuilder()
    StringBuilder allMavenOpts = new StringBuilder()
    boolean rootPOMAdded = false
    boolean perModuleEmailAdded = false
    boolean archivingDisabledAdded = false
    boolean runHeadlessAdded = false

    MavenHelper(List<WithXmlAction> withXmlActions, JobType type, JobManagement jobManagement) {
        super(withXmlActions, type)
        this.jobManagement = jobManagement
    }

    /**
     * Specifies the path to the root POM.
     * @param rootPOM path to the root POM
     */
    def rootPOM(String rootPOM) {
        checkState type == JobType.Maven, 'rootPOM can only be applied for Maven jobs'
        checkState !rootPOMAdded, 'rootPOM can only be applied once'
        rootPOMAdded = true
        execute { Node node ->
            appendOrReplaceNode node, 'rootPOM', rootPOM
        }
    }

    /**
     * Specifies the goals to execute.
     * @param goals the goals to execute
     */
    def goals(String goals) {
        checkState type == JobType.Maven, 'goals can only be applied for Maven jobs'
        if (allGoals.length() == 0) {
            allGoals.append goals
            execute { Node node ->
                appendOrReplaceNode node, 'goals', this.allGoals.toString()
            }
        } else {
            allGoals.append ' '
            allGoals.append goals
        }
    }

    /**
     * Specifies the JVM options needed when launching Maven as an external process.
     * @param mavenOpts JVM options needed when launching Maven
     */
    def mavenOpts(String mavenOpts) {
        checkState type == JobType.Maven, 'mavenOpts can only be applied for Maven jobs'
        if (allMavenOpts.length() == 0) {
            allMavenOpts.append mavenOpts
            execute { Node node ->
                appendOrReplaceNode node, 'mavenOpts', this.allMavenOpts.toString()
            }
        } else {
            allMavenOpts.append ' '
            allMavenOpts.append mavenOpts
        }
    }

    /**
     * If set, Jenkins will send an e-mail notifications for each module, defaults to <code>false</code>.
     * @param perModuleEmail set to <code>true</code> to enable per module e-mail notifications
     */
    def perModuleEmail(boolean perModuleEmail) {
        checkState type == JobType.Maven, 'perModuleEmail can only be applied for Maven jobs'
        checkState !perModuleEmailAdded, 'perModuleEmail can only be applied once'
        perModuleEmailAdded = true
        execute { Node node ->
            appendOrReplaceNode node, 'perModuleEmail', perModuleEmail
        }
    }

    /**
     * If set, Jenkins  will not automatically archive all artifacts generated by this project, defaults to
     * <code>false</code>.
     * @param archivingDisabled set to <code>true</code> to disable automatic archiving
     */
    def archivingDisabled(boolean archivingDisabled) {
        checkState type == JobType.Maven, 'archivingDisabled can only be applied for Maven jobs'
        checkState !archivingDisabledAdded, 'archivingDisabled can only be applied once'
        archivingDisabledAdded = true
        execute { Node node ->
            appendOrReplaceNode node, 'archivingDisabled', archivingDisabled
        }
    }

    /**
     * Set to allow Jenkins to configure the build process in headless mode, defaults to <code>false</code>.
     * @param runHeadless set to <code>true</code> to run the build process in headless mode
     */
    def runHeadless(boolean runHeadless) {
        checkState type == JobType.Maven, 'runHeadless can only be applied for Maven jobs'
        checkState !runHeadlessAdded, 'runHeadless can only be applied once'
        runHeadlessAdded = true
        execute { Node node ->
            appendOrReplaceNode node, 'runHeadless', runHeadless
        }
    }

    /**
     * <localRepository class="hudson.maven.local_repo.PerJobLocalRepositoryLocator"/>
     *
     * Set to use isolated local Maven repositories.
     * @param location the local repository to use for isolation
     */
    def localRepository(MavenContext.LocalRepositoryLocation location) {
        checkState type == JobType.Maven, 'localRepository can only be applied for Maven jobs'
        checkNotNull location, 'localRepository can not be null'
        execute { Node node ->
            appendOrReplaceNode node, 'localRepository', [class: location.type]
        }
    }

    def preBuildSteps(Closure preBuildClosure) {
        checkState type == JobType.Maven, 'prebuildSteps can only be applied for Maven jobs'
        AbstractStepContext preBuildContext = new AbstractStepContext(jobManagement)
        AbstractContextHelper.executeInContext(preBuildClosure, preBuildContext)

        if (!preBuildContext.stepNodes.isEmpty()) {
            execute { Node node ->
                appendOrReplaceNode(node, 'prebuilders', preBuildContext.stepNodes)
            }
        }
    }

    def postBuildSteps(Closure postBuildClosure) {
        checkState type == JobType.Maven, 'postBuildSteps can only be applied for Maven jobs'
        AbstractStepContext postBuildContext = new AbstractStepContext(jobManagement)
        AbstractContextHelper.executeInContext(postBuildClosure, postBuildContext)

        if (!postBuildContext.stepNodes.isEmpty()) {
            execute { Node node ->
                appendOrReplaceNode(node, 'postbuilders', postBuildContext.stepNodes)
            }
        }
    }

    def mavenInstallation(String name) {
        checkState type == JobType.Maven, 'mavenInstallation can only be applied for Maven jobs'
        checkNotNull name, 'name can not be null'
        execute { Node node ->
            appendOrReplaceNode node, 'mavenName', name
        }
    }

    def providedSettings(String settingsName) {
        String settingsId = jobManagement.getMavenSettingsId(settingsName)
        checkNotNull settingsId, "Managed Maven settings with name '${settingsName}' not found"

        execute { Node node ->
            node / settings(class: 'org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider') {
                settingsConfigId(settingsId)
            }
        }
    }
}
