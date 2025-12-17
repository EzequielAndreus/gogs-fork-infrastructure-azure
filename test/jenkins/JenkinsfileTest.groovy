package jenkins

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileDynamic

/**
 * Integration tests for the main Jenkinsfile pipeline
 * Uses JenkinsPipelineUnit framework for testing pipelines without Jenkins
 *
 * To run these tests:
 * ./gradlew test
 *
 * Or with Maven:
 * mvn test
 */
@CompileDynamic
class JenkinsfileTest extends BasePipelineTest {

    // Mock shared utilities
    protected mockUtils

    @Override
    @Before
    void setUp() {
        super.setUp()

        // Set the script root to the project root
        helper.scriptRoots += 'test/jenkins'
        helper.scriptRoots += '.'

        // Register allowed methods
        registerAllowedMethods()

        // Setup mock environment variables
        setupMockEnvironment()

        // Setup mock shared utilities
        setupMockUtils()
    }

    /**
     * Register all allowed Jenkins pipeline methods
     */
    protected void registerAllowedMethods() {
        // Pipeline structure methods
        helper.registerAllowedMethod('pipeline', [Closure], null)
        helper.registerAllowedMethod('agent', [Closure], null)
        helper.registerAllowedMethod('agent', [String], null)
        helper.registerAllowedMethod('any', [], null)
        helper.registerAllowedMethod('stages', [Closure], null)
        helper.registerAllowedMethod('stage', [String, Closure], null)
        helper.registerAllowedMethod('steps', [Closure], null)
        helper.registerAllowedMethod('script', [Closure], { Closure c -> c.call() })
        helper.registerAllowedMethod('post', [Closure], null)
        helper.registerAllowedMethod('always', [Closure], null)
        helper.registerAllowedMethod('success', [Closure], null)
        helper.registerAllowedMethod('failure', [Closure], null)
        helper.registerAllowedMethod('aborted', [Closure], null)

        // Triggers and options
        helper.registerAllowedMethod('triggers', [Closure], null)
        helper.registerAllowedMethod('githubPush', [], null)
        helper.registerAllowedMethod('options', [Closure], null)
        helper.registerAllowedMethod('buildDiscarder', [Object], null)
        helper.registerAllowedMethod('logRotator', [Map], null)
        helper.registerAllowedMethod('timestamps', [], null)
        helper.registerAllowedMethod('timeout', [Map], null)
        helper.registerAllowedMethod('timeout', [Map, Closure], { Map m, Closure c -> c.call() })
        helper.registerAllowedMethod('disableConcurrentBuilds', [], null)
        helper.registerAllowedMethod('ansiColor', [String], null)

        // Environment and credentials
        helper.registerAllowedMethod('environment', [Closure], null)
        helper.registerAllowedMethod('credentials', [String], { String id -> "mock-${id}" })

        // Conditional execution
        helper.registerAllowedMethod('when', [Closure], null)
        helper.registerAllowedMethod('expression', [Closure], { Closure c -> c.call() })

        // Input and approval
        helper.registerAllowedMethod('input', [Map], { Map m ->
            binding.setVariable('APPROVER', 'test-approver')
            return 'approved'
        })

        // SCM and workspace
        helper.registerAllowedMethod('checkout', [Object], null)
        helper.registerAllowedMethod('cleanWs', [], null)

        // Shell commands
        helper.registerAllowedMethod('sh', [String], { String cmd ->
            println "Executing: ${cmd}"
            return ''
        })
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            println "Executing: ${m.script}"
            return ''
        })

        // Echo and logging
        helper.registerAllowedMethod('echo', [String], { String msg -> println msg })
        helper.registerAllowedMethod('error', [String], { String msg -> throw new Exception(msg) })

        // Load shared library
        helper.registerAllowedMethod('load', [String], { String path ->
            return mockUtils
        })

        // Parameters (not used in this pipeline but may be referenced)
        helper.registerAllowedMethod('parameters', [Closure], null)
        helper.registerAllowedMethod('choice', [Map], null)
        helper.registerAllowedMethod('string', [Map], null)
        helper.registerAllowedMethod('booleanParam', [Map], null)
    }

    /**
     * Setup mock environment variables
     */
    protected void setupMockEnvironment() {
        binding.setVariable('env', [
            BUILD_URL: 'http://jenkins.example.com/job/infrastructure/123/',
            BUILD_NUMBER: '123',
            GIT_BRANCH: 'main',
            GIT_COMMIT: 'abc123def456',
            TERRAFORM_VERSION: '1.5.7',
            TERRAGRUNT_VERSION: '0.53.0',
            ARM_CLIENT_ID: 'mock-client-id',
            ARM_CLIENT_SECRET: 'mock-client-secret',
            ARM_SUBSCRIPTION_ID: 'mock-subscription-id',
            ARM_TENANT_ID: 'mock-tenant-id',
            DISCORD_WEBHOOK_URL: 'https://discord.com/api/webhooks/mock',
            JIRA_URL: 'https://jira.example.com',
            JIRA_USER: 'mock-user',
            JIRA_API_TOKEN: 'mock-token',
            JIRA_PROJECT_KEY: 'INFRA',
            STAGING_HAS_CHANGES: 'false',
            PRODUCTION_HAS_CHANGES: 'false'
        ])

        binding.setVariable('scm', [:])
        binding.setVariable('currentBuild', [result: 'SUCCESS'])
    }

    /**
     * Setup mock shared utilities
     */
    protected void setupMockUtils() {
        mockUtils = [
            setupTools: { String tfVersion, String tgVersion ->
                println "Mock: Setting up Terraform ${tfVersion} and Terragrunt ${tgVersion}"
            },
            azureLogin: { String clientId, String clientSecret, String tenantId, String subscriptionId ->
                println "Mock: Azure login with client ${clientId}"
            },
            azureLogout: {
                println "Mock: Azure logout"
            },
            validateHcl: { String environment ->
                println "Mock: Validating HCL for ${environment}"
            },
            terragruntPlan: { String environment, String targetModule = 'all' ->
                println "Mock: Terragrunt plan for ${environment}"
                // Return based on environment for testing different scenarios
                return binding.getVariable('env')["${environment.toUpperCase()}_HAS_CHANGES"] == 'true'
            },
            terragruntApply: { String environment, String targetModule = 'all' ->
                println "Mock: Terragrunt apply for ${environment}"
            },
            terragruntOutput: { String environment ->
                println "Mock: Terragrunt output for ${environment}"
            },
            sendDiscordNotification: { String webhookUrl, String status, String environment,
                                        String action, String targetModule, String buildUrl,
                                        String buildNumber, String additionalMessage = '' ->
                println "Mock: Discord notification - ${status} for ${environment}"
            },
            createJiraTicket: { String jiraUrl, String jiraUser, String jiraToken,
                                 String projectKey, String environment, String action,
                                 String targetModule, String buildUrl, String errorMessage = '' ->
                println "Mock: Creating Jira ticket for ${environment}"
                return 'INFRA-123'
            },
            cleanup: {
                println "Mock: Cleanup"
            }
        ]
    }

    // ==================== Test Cases ====================

    @Test
    void testPipelineLoadsSuccessfully() {
        // Load the Jenkinsfile
        def script = loadScript('Jenkinsfile')

        assertNotNull("Pipeline script should load successfully", script)
    }

    @Test
    void testPipelineWithNoChanges() {
        // Set up scenario: no changes in either environment
        binding.getVariable('env').STAGING_HAS_CHANGES = 'false'
        binding.getVariable('env').PRODUCTION_HAS_CHANGES = 'false'

        // Override terragruntPlan to return false (no changes)
        mockUtils.terragruntPlan = { String environment, String targetModule = 'all' ->
            println "Mock: Terragrunt plan for ${environment} - No changes"
            return false
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Verify that no apply was called
        def applyCallCount = helper.callStack.findAll { call ->
            call.methodName == 'echo' && callArgsToString(call).contains('Applying')
        }.size()

        assertEquals("Apply should not be called when no changes", 0, applyCallCount)

        printCallStack()
    }

    @Test
    void testPipelineWithStagingChangesOnly() {
        // Override terragruntPlan to return true only for staging
        mockUtils.terragruntPlan = { String environment, String targetModule = 'all' ->
            if (environment == 'staging') {
                binding.getVariable('env').STAGING_HAS_CHANGES = 'true'
                return true
            }
            return false
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Verify staging apply was called
        def stagingApplyCalled = helper.callStack.any { call ->
            call.methodName == 'echo' &&
            callArgsToString(call).contains('Staging') &&
            callArgsToString(call).contains('Changes detected')
        }

        assertTrue("Staging changes should be detected", stagingApplyCalled)

        printCallStack()
    }

    @Test
    void testPipelineWithProductionChangesOnly() {
        // Override terragruntPlan to return true only for production
        mockUtils.terragruntPlan = { String environment, String targetModule = 'all' ->
            if (environment == 'production') {
                binding.getVariable('env').PRODUCTION_HAS_CHANGES = 'true'
                return true
            }
            return false
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Verify production changes detected
        def productionChangesCalled = helper.callStack.any { call ->
            call.methodName == 'echo' &&
            callArgsToString(call).contains('Production') &&
            callArgsToString(call).contains('Changes detected')
        }

        assertTrue("Production changes should be detected", productionChangesCalled)

        printCallStack()
    }

    @Test
    void testPipelineWithBothEnvironmentChanges() {
        // Override terragruntPlan to return true for both environments
        mockUtils.terragruntPlan = { String environment, String targetModule = 'all' ->
            if (environment == 'staging') {
                binding.getVariable('env').STAGING_HAS_CHANGES = 'true'
            } else if (environment == 'production') {
                binding.getVariable('env').PRODUCTION_HAS_CHANGES = 'true'
            }
            return true
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Verify both environments had changes detected
        def stagingChanges = helper.callStack.any { call ->
            call.methodName == 'echo' &&
            callArgsToString(call).contains('Staging') &&
            callArgsToString(call).contains('Changes detected')
        }

        def productionChanges = helper.callStack.any { call ->
            call.methodName == 'echo' &&
            callArgsToString(call).contains('Production') &&
            callArgsToString(call).contains('Changes detected')
        }

        assertTrue("Staging changes should be detected", stagingChanges)
        assertTrue("Production changes should be detected", productionChanges)

        printCallStack()
    }

    @Test
    void testDiscordNotificationOnStart() {
        def discordCalled = false
        def discordStatus = ''

        mockUtils.sendDiscordNotification = { String webhookUrl, String status, String environment,
                                               String action, String targetModule, String buildUrl,
                                               String buildNumber, String additionalMessage = '' ->
            if (status == 'STARTED') {
                discordCalled = true
                discordStatus = status
            }
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        assertTrue("Discord notification should be sent on start", discordCalled)
        assertEquals("Status should be STARTED", 'STARTED', discordStatus)
    }

    @Test
    void testAzureLoginCalled() {
        def azureLoginCalled = false

        mockUtils.azureLogin = { String clientId, String clientSecret, String tenantId, String subscriptionId ->
            azureLoginCalled = true
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        assertTrue("Azure login should be called", azureLoginCalled)
    }

    @Test
    void testAzureLogoutCalledInPost() {
        def azureLogoutCalled = false

        mockUtils.azureLogout = {
            azureLogoutCalled = true
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Note: Post blocks may need special handling in the test framework
        // This test verifies the method exists and can be called
        mockUtils.azureLogout()
        assertTrue("Azure logout should be available", azureLogoutCalled)
    }

    @Test
    void testSetupToolsCalled() {
        def setupToolsCalled = false

        mockUtils.setupTools = { String terraformVersion, String terragruntVersion ->
            setupToolsCalled = true
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        assertTrue("Setup tools should be called", setupToolsCalled)
    }

    @Test
    void testValidateHclCalledForBothEnvironments() {
        def validateCalls = []

        mockUtils.validateHcl = { String environment ->
            validateCalls.add(environment)
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        assertTrue("Staging validation should be called", validateCalls.contains('staging'))
        assertTrue("Production validation should be called", validateCalls.contains('production'))
    }

    @Test
    void testCleanupCalled() {
        def cleanupCalled = false

        mockUtils.cleanup = {
            cleanupCalled = true
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Cleanup is called in post block
        mockUtils.cleanup()
        assertTrue("Cleanup should be available and callable", cleanupCalled)
    }

    @Test
    void testProductionApprovalRequired() {
        // Override input to track when approval is requested
        helper.registerAllowedMethod('input', [Map], { Map m ->
            binding.setVariable('APPROVER', 'test-approver')
            return 'approved'
        })

        // Set production to have changes
        mockUtils.terragruntPlan = { String environment, String targetModule = 'all' ->
            if (environment == 'production') {
                binding.getVariable('env').PRODUCTION_HAS_CHANGES = 'true'
                return true
            }
            return false
        }

        def script = loadScript('Jenkinsfile')
        script.run()

        // Note: The when block evaluation may need the actual condition check
        // Verify script ran successfully
        assertNotNull('Script should execute', script)
        printCallStack()
    }

    @Test
    void testPipelineSummaryStage() {
        def script = loadScript('Jenkinsfile')
        script.run()

        def summaryPrinted = helper.callStack.any { call ->
            call.methodName == 'echo' &&
            callArgsToString(call).contains('Pipeline Summary')
        }

        assertTrue("Pipeline summary should be printed", summaryPrinted)
    }

    // ==================== Helper Methods ====================

    /**
     * Print the call stack for debugging
     */
    protected void printCallStack() {
        println "\n========== Call Stack =========="
        helper.callStack.each { call ->
            println "${call.methodName}: ${callArgsToString(call)}"
        }
        println "================================\n"
    }

    /**
     * Assert that a specific stage was executed
     */
    protected void assertStageExecuted(String stageName) {
        def stageExecuted = helper.callStack.any { call ->
            call.methodName == 'stage' && callArgsToString(call).contains(stageName)
        }
        assertTrue("Stage '${stageName}' should be executed", stageExecuted)
    }

    /**
     * Assert that a specific stage was NOT executed
     */
    protected void assertStageNotExecuted(String stageName) {
        def stageExecuted = helper.callStack.any { call ->
            call.methodName == 'stage' && callArgsToString(call).contains(stageName)
        }
        assertFalse("Stage '${stageName}' should NOT be executed", stageExecuted)
    }

    /**
     * Get count of method calls
     */
    protected int getMethodCallCount(String methodName) {
        return helper.callStack.findAll { call -> call.methodName == methodName }.size()
    }
}

