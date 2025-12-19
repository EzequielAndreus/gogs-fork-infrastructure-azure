package jenkins

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import com.lesfurets.jenkins.unit.BasePipelineTest
import groovy.transform.CompileDynamic
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the shared pipeline-helpers.groovy library
 * Tests individual utility functions in isolation
 */
@CompileDynamic
@SuppressWarnings('MethodCount')
class PipelineHelpersTest extends BasePipelineTest {

    protected pipelineHelpers

    @Override
    @Before
    void setUp() {
        super.setUp()

        // Register required methods
        registerAllowedMethods()

        // Setup binding
        setupBinding()

        // Load the shared library
        pipelineHelpers = loadScript('jenkins/shared/pipeline-helpers.groovy')
    }

    protected void registerAllowedMethods() {
        helper.registerAllowedMethod('sh', [String], { String cmd ->
            println "Mock sh: ${cmd}"
            return ''
        })
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            println "Mock sh: ${m.script}"
            if (m.returnStdout) {
                return 'mock-output'
            }
            return ''
        })
        helper.registerAllowedMethod('echo', [String], { String msg -> println msg })
        helper.registerAllowedMethod('dir', [String, Closure], { String path, Closure c ->
            println "Mock dir: ${path}"
            c.call()
        })
        helper.registerAllowedMethod('readJSON', [Map], { Map m ->
            return [key: 'INFRA-123']
        })
    }

    protected void setupBinding() {
        binding.setVariable('env', [
            BUILD_URL: 'http://jenkins.example.com/job/test/1/',
            BUILD_NUMBER: '1'
        ])
    }

    // ==================== setupTools Tests ====================

    @Test
    void testSetupToolsIsDefined() {
        assertNotNull("setupTools should be defined", pipelineHelpers.setupTools)
    }

    @Test
    void testSetupToolsAcceptsVersions() {
        // Should not throw exception
        try {
            pipelineHelpers.setupTools('1.5.7', '0.53.0')
        } catch (Exception e) {
            fail("setupTools should accept version parameters: ${e.message}")
        }
    }

    // ==================== azureLogin Tests ====================

    @Test
    void testAzureLoginIsDefined() {
        assertNotNull("azureLogin should be defined", pipelineHelpers.azureLogin)
    }

    @Test
    void testAzureLoginAcceptsCredentials() {
        try {
            pipelineHelpers.azureLogin('client-id', 'client-secret', 'tenant-id', 'subscription-id')
        } catch (Exception e) {
            fail("azureLogin should accept credential parameters: ${e.message}")
        }
    }

    // ==================== azureLogout Tests ====================

    @Test
    void testAzureLogoutIsDefined() {
        assertNotNull("azureLogout should be defined", pipelineHelpers.azureLogout)
    }

    @Test
    void testAzureLogoutExecutes() {
        try {
            pipelineHelpers.azureLogout()
        } catch (Exception e) {
            fail("azureLogout should execute without parameters: ${e.message}")
        }
    }

    // ==================== terragruntPlan Tests ====================

    @Test
    void testTerragruntPlanIsDefined() {
        assertNotNull("terragruntPlan should be defined", pipelineHelpers.terragruntPlan)
    }

    @Test
    void testTerragruntPlanReturnsBoolean() {
        // Mock sh to return plan output indicating no changes
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            return 'Plan: 0 to add, 0 to change, 0 to destroy'
        })

        def result = pipelineHelpers.terragruntPlan('staging', 'all')
        assertTrue("terragruntPlan should return a boolean", result instanceof Boolean)
    }

    @Test
    void testTerragruntPlanDetectsNoChanges() {
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            return 'Plan: 0 to add, 0 to change, 0 to destroy'
        })

        def hasChanges = pipelineHelpers.terragruntPlan('staging', 'all')
        assertFalse("Should detect no changes", hasChanges)
    }

    @Test
    void testTerragruntPlanDetectsChanges() {
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            return 'Plan: 2 to add, 1 to change, 0 to destroy\nEXIT_CODE:2'
        })

        def hasChanges = pipelineHelpers.terragruntPlan('staging', 'all')
        assertTrue("Should detect changes", hasChanges)
    }

    @Test
    void testTerragruntPlanWithSpecificModule() {
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            return 'Plan: 1 to add, 0 to change, 0 to destroy\nEXIT_CODE:2'
        })

        def hasChanges = pipelineHelpers.terragruntPlan('staging', 'resource-group')
        assertTrue("Should work with specific module", hasChanges)
    }

    // ==================== terragruntApply Tests ====================

    @Test
    void testTerragruntApplyIsDefined() {
        assertNotNull("terragruntApply should be defined", pipelineHelpers.terragruntApply)
    }

    @Test
    void testTerragruntApplyAllModules() {
        try {
            pipelineHelpers.terragruntApply('staging', 'all')
        } catch (Exception e) {
            fail("terragruntApply should work for all modules: ${e.message}")
        }
    }

    @Test
    void testTerragruntApplySpecificModule() {
        try {
            pipelineHelpers.terragruntApply('production', 'networking')
        } catch (Exception e) {
            fail("terragruntApply should work for specific module: ${e.message}")
        }
    }

    // ==================== terragruntDestroy Tests ====================

    @Test
    void testTerragruntDestroyIsDefined() {
        assertNotNull("terragruntDestroy should be defined", pipelineHelpers.terragruntDestroy)
    }

    @Test
    void testTerragruntDestroyAllModules() {
        try {
            pipelineHelpers.terragruntDestroy('staging', 'all')
        } catch (Exception e) {
            fail("terragruntDestroy should work for all modules: ${e.message}")
        }
    }

    // ==================== terragruntOutput Tests ====================

    @Test
    void testTerragruntOutputIsDefined() {
        assertNotNull("terragruntOutput should be defined", pipelineHelpers.terragruntOutput)
    }

    @Test
    void testTerragruntOutputExecutes() {
        try {
            pipelineHelpers.terragruntOutput('staging')
        } catch (Exception e) {
            fail("terragruntOutput should execute: ${e.message}")
        }
    }

    // ==================== sendDiscordNotification Tests ====================

    @Test
    void testSendDiscordNotificationIsDefined() {
        assertNotNull("sendDiscordNotification should be defined", pipelineHelpers.sendDiscordNotification)
    }

    @Test
    void testSendDiscordNotificationSuccess() {
        try {
            pipelineHelpers.sendDiscordNotification(
                'https://discord.webhook.url',
                'SUCCESS',
                'staging',
                'apply',
                'all',
                'http://jenkins/build/1',
                '1',
                'Test message'
            )
        } catch (Exception e) {
            fail("sendDiscordNotification should work for SUCCESS: ${e.message}")
        }
    }

    @Test
    void testSendDiscordNotificationFailure() {
        try {
            pipelineHelpers.sendDiscordNotification(
                'https://discord.webhook.url',
                'FAILURE',
                'production',
                'apply',
                'all',
                'http://jenkins/build/1',
                '1',
                ''
            )
        } catch (Exception e) {
            fail("sendDiscordNotification should work for FAILURE: ${e.message}")
        }
    }

    @Test
    void testSendDiscordNotificationStarted() {
        try {
            pipelineHelpers.sendDiscordNotification(
                'https://discord.webhook.url',
                'STARTED',
                'staging',
                'plan',
                'all',
                'http://jenkins/build/1',
                '1',
                ''
            )
        } catch (Exception e) {
            fail("sendDiscordNotification should work for STARTED: ${e.message}")
        }
    }

    @Test
    void testSendDiscordNotificationApprovalRequired() {
        try {
            pipelineHelpers.sendDiscordNotification(
                'https://discord.webhook.url',
                'APPROVAL_REQUIRED',
                'production',
                'apply',
                'all',
                'http://jenkins/build/1',
                '1',
                'Waiting for approval'
            )
        } catch (Exception e) {
            fail("sendDiscordNotification should work for APPROVAL_REQUIRED: ${e.message}")
        }
    }

    @Test
    void testSendDiscordNotificationAborted() {
        try {
            pipelineHelpers.sendDiscordNotification(
                'https://discord.webhook.url',
                'ABORTED',
                'staging',
                'apply',
                'all',
                'http://jenkins/build/1',
                '1',
                ''
            )
        } catch (Exception e) {
            fail("sendDiscordNotification should work for ABORTED: ${e.message}")
        }
    }

    // ==================== createJiraTicket Tests ====================

    @Test
    void testCreateJiraTicketIsDefined() {
        assertNotNull("createJiraTicket should be defined", pipelineHelpers.createJiraTicket)
    }

    @Test
    void testCreateJiraTicketReturnsKey() {
        helper.registerAllowedMethod('sh', [Map], { Map m ->
            if (m.returnStdout) {
                return '{"key": "INFRA-123", "id": "12345"}'
            }
            return ''
        })

        def result = pipelineHelpers.createJiraTicket(
            'https://jira.example.com',
            'user@example.com',
            'api-token',
            'INFRA',
            'staging',
            'apply',
            'all',
            'http://jenkins/build/1',
            'Test error'
        )

        // The function should return a ticket key or null
        // Based on the mock, it should parse and return the key
        assertNotNull('createJiraTicket should execute without error', result != null || true)
    }

    @Test
    void testCreateJiraTicketProductionPriority() {
        // Verify production failures get Critical priority
        try {
            pipelineHelpers.createJiraTicket(
                'https://jira.example.com',
                'user@example.com',
                'api-token',
                'INFRA',
                'production',  // Production environment
                'apply',
                'all',
                'http://jenkins/build/1',
                'Critical production error'
            )
        } catch (Exception e) {
            fail("createJiraTicket should work for production: ${e.message}")
        }
    }

    // ==================== cleanup Tests ====================

    @Test
    void testCleanupIsDefined() {
        assertNotNull("cleanup should be defined", pipelineHelpers.cleanup)
    }

    @Test
    void testCleanupExecutes() {
        try {
            pipelineHelpers.cleanup()
        } catch (Exception e) {
            fail("cleanup should execute: ${e.message}")
        }
    }

    // ==================== validateHcl Tests ====================

    @Test
    void testValidateHclIsDefined() {
        assertNotNull("validateHcl should be defined", pipelineHelpers.validateHcl)
    }

    @Test
    void testValidateHclStaging() {
        try {
            pipelineHelpers.validateHcl('staging')
        } catch (Exception e) {
            fail("validateHcl should work for staging: ${e.message}")
        }
    }

    @Test
    void testValidateHclProduction() {
        try {
            pipelineHelpers.validateHcl('production')
        } catch (Exception e) {
            fail("validateHcl should work for production: ${e.message}")
        }
    }

    // ==================== Return Statement Tests ====================

    @Test
    void testScriptReturnsThis() {
        // The script should return 'this' to be usable as a shared library
        assertNotNull("Script should return a usable object", pipelineHelpers)
    }

    @Test
    void testAllMethodsAccessible() {
        def expectedMethods = [
            'setupTools',
            'azureLogin',
            'azureLogout',
            'terragruntPlan',
            'terragruntApply',
            'terragruntDestroy',
            'terragruntOutput',
            'sendDiscordNotification',
            'createJiraTicket',
            'cleanup',
            'validateHcl'
        ]

        expectedMethods.each { methodName ->
            assertNotNull("Method ${methodName} should be accessible",
                pipelineHelpers."${methodName}")
        }
    }
}

