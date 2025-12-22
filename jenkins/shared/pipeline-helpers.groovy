//------------------------------------------------------------------------------
// Shared Jenkins Library Functions
// Common functions used by staging and production pipelines
//------------------------------------------------------------------------------

/**
 * Setup Terraform and Terragrunt tools
 * @param terraformVersion The Terraform version to install
 * @param terragruntVersion The Terragrunt version to install
 */
def setupTools(String terraformVersion, String terragruntVersion) {
    // Install Terraform
    sh """
        if ! command -v terraform &> /dev/null || [ "\$(terraform version -json | jq -r '.terraform_version' 2>/dev/null)" != "${terraformVersion}" ]; then
            echo "Installing Terraform ${terraformVersion}..."
            wget -q https://releases.hashicorp.com/terraform/${terraformVersion}/terraform_${terraformVersion}_linux_amd64.zip
            unzip -o terraform_${terraformVersion}_linux_amd64.zip
            sudo mv terraform /usr/local/bin/
            rm -f terraform_${terraformVersion}_linux_amd64.zip
        fi
        terraform version
    """

    // Install Terragrunt
    sh """
        if ! command -v terragrunt &> /dev/null; then
            echo "Installing Terragrunt ${terragruntVersion}..."
            wget -q https://github.com/gruntwork-io/terragrunt/releases/download/v${terragruntVersion}/terragrunt_linux_amd64
            chmod +x terragrunt_linux_amd64
            sudo mv terragrunt_linux_amd64 /usr/local/bin/terragrunt
        fi
        terragrunt --version
    """
}

/**
 * Login to Azure using Service Principal
 * @param clientId Azure Client ID
 * @param clientSecret Azure Client Secret
 * @param tenantId Azure Tenant ID
 * @param subscriptionId Azure Subscription ID
 */
def azureLogin(String clientId, String clientSecret, String tenantId, String subscriptionId) {
    sh """
        echo "Logging into Azure..."
        az login --service-principal \
            --username ${clientId} \
            --password ${clientSecret} \
            --tenant ${tenantId}
        az account set --subscription ${subscriptionId}
        echo "Successfully logged into Azure"
        az account show
    """
}

/**
 * Logout from Azure
 */
def azureLogout() {
    sh 'az logout || true'
}

/**
 * Run Terragrunt plan for all modules or a specific module
 * @param environment The environment (staging/production)
 * @param targetModule The module to target or 'all'
 * @return boolean True if there are changes to apply, false otherwise
 */
def terragruntPlan(String environment, String targetModule = 'all') {
    def hasChanges = false
    dir("environments/${environment}") {
        if (targetModule == 'all') {
            def planOutput = sh(
                script: '''
                    echo "Running Terragrunt plan for all modules..."
                    terragrunt run-all plan \
                        --terragrunt-non-interactive \
                        --terragrunt-include-external-dependencies \
                        -detailed-exitcode \
                        -out=tfplan 2>&1 || echo "EXIT_CODE:$?"
                ''',
                returnStdout: true
            ).trim()
            echo planOutput
            // Exit code 2 means there are changes to apply
            hasChanges = planOutput.contains('EXIT_CODE:2') || 
                         planOutput.contains('Plan:') && 
                         !planOutput.contains('Plan: 0 to add, 0 to change, 0 to destroy')
        } else {
            dir("${targetModule}") {
                def planOutput = sh(
                    script: """
                        echo "Running Terragrunt plan for ${targetModule}..."
                        terragrunt plan \
                            --terragrunt-non-interactive \
                            -detailed-exitcode \
                            -out=tfplan 2>&1 || echo "EXIT_CODE:\$?"
                    """,
                    returnStdout: true
                ).trim()
                echo planOutput
                hasChanges = planOutput.contains('EXIT_CODE:2') || 
                             planOutput.contains('Plan:') && 
                             !planOutput.contains('Plan: 0 to add, 0 to change, 0 to destroy')
            }
        }
    }
    return hasChanges
}

/**
 * Run Terragrunt apply for all modules or a specific module
 * @param environment The environment (staging/production)
 * @param targetModule The module to target or 'all'
 */
def terragruntApply(String environment, String targetModule = 'all') {
    dir("environments/${environment}") {
        if (targetModule == 'all') {
            sh '''
                echo "Applying Terragrunt changes for all modules..."
                terragrunt run-all apply \
                    --terragrunt-non-interactive \
                    --terragrunt-include-external-dependencies \
                    -auto-approve
            '''
        } else {
            dir("${targetModule}") {
                sh """
                    echo "Applying Terragrunt changes for ${targetModule}..."
                    terragrunt apply \
                        --terragrunt-non-interactive \
                        -auto-approve
                """
            }
        }
    }
}

/**
 * Run Terragrunt destroy for all modules or a specific module
 * @param environment The environment (staging/production)
 * @param targetModule The module to target or 'all'
 */
def terragruntDestroy(String environment, String targetModule = 'all') {
    dir("environments/${environment}") {
        if (targetModule == 'all') {
            sh """
                echo "⚠️ DESTROYING all resources in ${environment}..."
                terragrunt run-all destroy \
                    --terragrunt-non-interactive \
                    --terragrunt-include-external-dependencies \
                    -auto-approve
            """
        } else {
            dir("${targetModule}") {
                sh """
                    echo "⚠️ DESTROYING ${targetModule} in ${environment}..."
                    terragrunt destroy \
                        --terragrunt-non-interactive \
                        -auto-approve
                """
            }
        }
    }
}

/**
 * Get Terragrunt outputs
 * @param environment The environment (staging/production)
 */
def terragruntOutput(String environment) {
    dir("environments/${environment}") {
        sh '''
            echo "Fetching Terragrunt outputs..."
            terragrunt run-all output --terragrunt-non-interactive || true
        '''
    }
}

/**
 * Send Discord notification
 * @param webhookUrl Discord webhook URL
 * @param status Status of the build (SUCCESS, FAILURE, STARTED, APPROVAL_REQUIRED, ABORTED)
 * @param environment Environment name
 * @param action Action performed (plan, apply, destroy)
 * @param targetModule Target module
 * @param buildUrl Jenkins build URL
 * @param buildNumber Jenkins build number
 * @param additionalMessage Optional additional message
 */
def sendDiscordNotification(String webhookUrl, String status, String environment, String action, String targetModule, String buildUrl, String buildNumber, String additionalMessage = '') {
    def color
    def emoji
    def title
    
    switch(status) {
        case 'SUCCESS':
            color = 3066993  // Green
            emoji = '✅'
            title = "Infrastructure ${action.toUpperCase()} Successful"
            break
        case 'FAILURE':
            color = 15158332  // Red
            emoji = '❌'
            title = "Infrastructure ${action.toUpperCase()} Failed"
            break
        case 'STARTED':
            color = 3447003  // Blue
            emoji = '🚀'
            title = "Infrastructure ${action.toUpperCase()} Started"
            break
        case 'APPROVAL_REQUIRED':
            color = 16776960  // Yellow
            emoji = '⏳'
            title = "Approval Required"
            break
        case 'ABORTED':
            color = 16776960  // Yellow
            emoji = '⚠️'
            title = "Pipeline Aborted"
            break
        default:
            color = 9807270  // Gray
            emoji = 'ℹ️'
            title = "Infrastructure Update"
    }
    
    def fields = """
                {
                    "name": "Environment",
                    "value": "${environment}",
                    "inline": true
                },
                {
                    "name": "Action",
                    "value": "${action}",
                    "inline": true
                },
                {
                    "name": "Module",
                    "value": "${targetModule}",
                    "inline": true
                },
                {
                    "name": "Build",
                    "value": "[#${buildNumber}](${buildUrl})",
                    "inline": true
                }
    """
    
    if (additionalMessage) {
        fields += """,
                {
                    "name": "Details",
                    "value": "${additionalMessage}",
                    "inline": false
                }
        """
    }
    
    def payload = """
    {
        "embeds": [{
            "title": "${emoji} ${title}",
            "color": ${color},
            "fields": [${fields}
            ],
            "footer": {
                "text": "Gogs Infrastructure Azure"
            },
            "timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC'))}"
        }]
    }
    """
    
    sh """
        curl -s -X POST -H "Content-Type: application/json" \
            -d '${payload}' \
            "${webhookUrl}" || true
    """
}

/**
 * Create Jira ticket for pipeline failures
 * @param jiraUrl Jira instance URL
 * @param jiraUser Jira username/email
 * @param jiraToken Jira API token
 * @param projectKey Jira project key
 * @param environment Environment name
 * @param action Action that failed
 * @param targetModule Target module
 * @param buildUrl Jenkins build URL
 * @param errorMessage Error message or details
 */
def createJiraTicket(String jiraUrl, String jiraUser, String jiraToken, String projectKey, String environment, String action, String targetModule, String buildUrl, String errorMessage = '') {
    def priority = environment == 'production' ? 'Critical' : 'High'
    def summary = "Infrastructure ${action} Failed - ${environment.toUpperCase()}"
    
    def description = """
h2. Infrastructure Pipeline Failure

*Environment:* ${environment}
*Action:* ${action}
*Module:* ${targetModule}
*Build URL:* ${buildUrl}

h3. Error Details
{code}
${errorMessage ?: 'See Jenkins build logs for details'}
{code}

h3. Next Steps
# Review the Jenkins build logs
# Identify the root cause
# Fix the issue and re-run the pipeline
    """.trim()
    
    // Escape special characters for JSON
    def escapedDescription = description.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
    
    def payload = """
    {
        "fields": {
            "project": {
                "key": "${projectKey}"
            },
            "summary": "${summary}",
            "description": "${escapedDescription}",
            "issuetype": {
                "name": "Bug"
            },
            "priority": {
                "name": "${priority}"
            },
            "labels": ["infrastructure", "terraform", "${environment}", "auto-created"]
        }
    }
    """
    
    def response = sh(
        script: """
            curl -s -X POST \
                -H "Content-Type: application/json" \
                -u "${jiraUser}:${jiraToken}" \
                -d '${payload}' \
                "${jiraUrl}/rest/api/2/issue"
        """,
        returnStdout: true
    ).trim()
    
    echo "Jira ticket created: ${response}"
    
    // Try to extract ticket key
    try {
        def jsonResponse = readJSON(text: response)
        if (jsonResponse.key) {
            echo "Created Jira ticket: ${jsonResponse.key}"
            return jsonResponse.key
        }
    } catch (Exception e) {
        echo "Could not parse Jira response: ${e.message}"
    }
    
    return null
}

/**
 * Clean up sensitive files after pipeline execution
 */
def cleanup() {
    sh '''
        find . -name "*.tfplan" -delete || true
        find . -name "tfplan" -delete || true
        find . -name ".terraform.lock.hcl" -delete || true
    '''
}

/**
 * Validate Terragrunt HCL format
 * @param environment The environment to validate
 */
def validateHcl(String environment) {
    dir("environments/${environment}") {
        sh '''
            echo "Validating Terragrunt configuration..."
            terragrunt hclfmt --terragrunt-check || true
        '''
    }
}

return this
