# Jenkins Pipeline Integration Tests

This directory contains integration tests for the Jenkins pipeline using the [Jenkins Pipeline Unit](https://github.com/jenkinsci/JenkinsPipelineUnit) testing framework.

## Overview

The tests validate the main Jenkinsfile pipeline and shared library functions without requiring a running Jenkins instance.

## Test Files

| File | Description |
| ---- | ----------- |
| `JenkinsfileTest.groovy` | Integration tests for the main Jenkinsfile pipeline |
| `PipelineHelpersTest.groovy` | Unit tests for the shared `pipeline-helpers.groovy` library |

## Prerequisites

- Java 11+ (JDK)
- Gradle 8.x (or use the Gradle wrapper)

## Running Tests

### Using Gradle

```bash
# Run all tests
./gradlew test

# Run only Jenkins pipeline tests
./gradlew testJenkinsPipeline

# Run with verbose output
./gradlew test --info

# Run a specific test class
./gradlew test --tests "jenkins.JenkinsfileTest"

# Run a specific test method
./gradlew test --tests "jenkins.JenkinsfileTest.testPipelineWithNoChanges"
```

### Using IDE

Import the project as a Gradle project in IntelliJ IDEA or Eclipse, then run tests directly from the IDE.

## Test Coverage

### JenkinsfileTest

| Test | Description |
| ---- | ----------- |
| `testPipelineLoadsSuccessfully` | Verifies the Jenkinsfile loads without errors |
| `testPipelineWithNoChanges` | Tests pipeline when no infrastructure changes detected |
| `testPipelineWithStagingChangesOnly` | Tests pipeline with only staging changes |
| `testPipelineWithProductionChangesOnly` | Tests pipeline with only production changes |
| `testPipelineWithBothEnvironmentChanges` | Tests pipeline with changes in both environments |
| `testDiscordNotificationOnStart` | Verifies Discord notification on pipeline start |
| `testAzureLoginCalled` | Verifies Azure login is invoked |
| `testSetupToolsCalled` | Verifies Terraform/Terragrunt setup |
| `testValidateHclCalledForBothEnvironments` | Verifies HCL validation for both envs |
| `testProductionApprovalRequired` | Verifies approval gate for production |
| `testPipelineSummaryStage` | Verifies summary stage execution |

### PipelineHelpersTest

| Test Category | Tests |
| ------------- | ----- |
| `setupTools` | Definition, version parameter acceptance |
| `azureLogin` | Definition, credential acceptance |
| `azureLogout` | Definition, execution |
| `terragruntPlan` | Returns boolean, detects changes/no-changes, module targeting |
| `terragruntApply` | All modules, specific modules |
| `terragruntDestroy` | All modules, specific modules |
| `terragruntOutput` | Execution |
| `sendDiscordNotification` | SUCCESS, FAILURE, STARTED, APPROVAL_REQUIRED, ABORTED |
| `createJiraTicket` | Returns key, production priority |
| `cleanup` | Execution |
| `validateHcl` | Staging, production |

## How It Works

### Mocking Strategy

1. **Pipeline Methods**: All Jenkins pipeline methods (`sh`, `echo`, `checkout`, etc.) are mocked using `helper.registerAllowedMethod()`

2. **Shared Library**: The `load` method is mocked to return a mock utils object with stub implementations

3. **Environment Variables**: `env` binding is populated with mock values for credentials and build info

4. **Credentials**: The `credentials()` function returns mock values prefixed with `mock-`

### Test Structure

```groovy
class JenkinsfileTest extends BasePipelineTest {
    @Before
    void setUp() {
        super.setUp()
        // Register mocks
        // Setup environment
    }
    
    @Test
    void testSomething() {
        def script = loadScript('Jenkinsfile')
        script.run()
        
        // Assertions on call stack
        assertTrue(...)
    }
}
```

## Debugging

### Print Call Stack

Use `printCallStack()` helper method to see all method calls:

```groovy
@Test
void testWithDebug() {
    def script = loadScript('Jenkinsfile')
    script.run()
    printCallStack()  // Prints all calls
}
```

### Check Specific Calls

```groovy
// Find calls to a specific method
def shCalls = helper.callStack.findAll { it.methodName == 'sh' }

// Check if echo was called with specific text
def foundEcho = helper.callStack.any { call ->
    call.methodName == 'echo' && 
    callArgsToString(call).contains('expected text')
}
```

## Adding New Tests

1. Add test method to appropriate test class
2. Follow naming convention: `test<Feature><Scenario>`
3. Use `loadScript()` to load the pipeline
4. Call `script.run()` to execute
5. Assert on `helper.callStack` for verification

## Troubleshooting

### Script Not Found

Ensure `scriptRoots` includes the correct paths:
```groovy
helper.scriptRoots += '.'
helper.scriptRoots += 'jenkins/shared'
```

### Method Not Allowed

Register the method:
```groovy
helper.registerAllowedMethod('methodName', [ParamType.class], { ... })
```

### Closure Execution

For closures that need to execute:
```groovy
helper.registerAllowedMethod('script', [Closure.class], { Closure c -> c.call() })
```
