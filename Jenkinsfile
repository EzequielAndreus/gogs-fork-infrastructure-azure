/*
 * Jenkins CD Pipeline for Infrastructure Provisioning
 * 
 * This pipeline automatically provisions/updates both staging and production
 * environments based on detected infrastructure changes from Terragrunt plan.
 * 
 * Flow:
 * 1. Plan staging → Apply if changes detected
 * 2. Plan production → Apply if changes detected (with approval)
 * 
 * Shared utility functions are in:
 * - jenkins/shared/InfraUtils.groovy
 * 
 * Prerequisites:
 * - Azure Service Principal credentials stored in Jenkins
 * - Terraform and Terragrunt installed on Jenkins agents
 * - Azure CLI installed on Jenkins agents
 */

// Load shared utilities
def utils

pipeline {
    agent any
    
    triggers {
        // Webhook trigger from GitHub on merge to main
        githubPush()
    }
    
    environment {
        // Tool versions
        TERRAFORM_VERSION = '1.5.7'
        TERRAGRUNT_VERSION = '0.53.0'
        
        // Azure credentials
        ARM_CLIENT_ID = credentials('azure-client-id')
        ARM_CLIENT_SECRET = credentials('azure-client-secret')
        ARM_SUBSCRIPTION_ID = credentials('azure-subscription-id')
        ARM_TENANT_ID = credentials('azure-tenant-id')
        
        // Terraform state configuration
        TF_STATE_RESOURCE_GROUP = credentials('tf-state-resource-group')
        TF_STATE_STORAGE_ACCOUNT = credentials('tf-state-storage-account')
        TF_STATE_CONTAINER = credentials('tf-state-container')
        
        // Application credentials - Staging
        TF_VAR_unique_suffix = credentials('tf-unique-suffix')
        TF_VAR_db_admin_username = credentials('db-admin-username')
        TF_VAR_db_admin_password = credentials('db-admin-password')
        TF_VAR_dockerhub_username = credentials('dockerhub-username')
        TF_VAR_dockerhub_password = credentials('dockerhub-password')
        TF_VAR_splunk_ssh_public_key = credentials('splunk-ssh-public-key')
        TF_VAR_docker_image = credentials('docker-image')
        
        // Discord webhook for notifications
        DISCORD_WEBHOOK_URL = credentials('discord-webhook-url')
        
        // Jira credentials for ticket creation on failure
        JIRA_URL = credentials('jira-url')
        JIRA_USER = credentials('jira-user')
        JIRA_API_TOKEN = credentials('jira-api-token')
        JIRA_PROJECT_KEY = credentials('jira-project-key')
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
        timeout(time: 3, unit: 'HOURS')
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    
    stages {
        stage('Initialize') {
            steps {
                checkout scm
                script {
                    // Load shared utility functions
                    utils = load 'jenkins/shared/InfraUtils.groovy'
                    
                    // Track changes for each environment
                    env.STAGING_HAS_CHANGES = 'false'
                    env.PRODUCTION_HAS_CHANGES = 'false'
                    
                    echo "============================================"
                    echo "  Infrastructure CD Pipeline"
                    echo "============================================"
                    echo "Branch: ${env.GIT_BRANCH ?: 'N/A'}"
                    echo "Commit: ${env.GIT_COMMIT ?: 'N/A'}"
                    echo "============================================"
                    
                    utils.sendDiscordNotification(
                        env.DISCORD_WEBHOOK_URL,
                        'STARTED',
                        'all',
                        'plan',
                        'all',
                        env.BUILD_URL,
                        env.BUILD_NUMBER,
                        'Infrastructure pipeline started'
                    )
                }
            }
        }
        
        stage('Setup Tools') {
            steps {
                script {
                    utils.setupTools(env.TERRAFORM_VERSION, env.TERRAGRUNT_VERSION)
                }
            }
        }
        
        stage('Azure Login') {
            steps {
                script {
                    utils.azureLogin(
                        env.ARM_CLIENT_ID,
                        env.ARM_CLIENT_SECRET,
                        env.ARM_TENANT_ID,
                        env.ARM_SUBSCRIPTION_ID
                    )
                }
            }
        }
        
        // ==================== STAGING ====================
        stage('Staging: Validate') {
            steps {
                script {
                    utils.validateHcl('staging')
                }
            }
        }
        
        stage('Staging: Plan') {
            steps {
                script {
                    echo "============================================"
                    echo "  Planning Staging Infrastructure"
                    echo "============================================"
                    
                    def hasChanges = utils.terragruntPlan('staging', 'all')
                    env.STAGING_HAS_CHANGES = hasChanges.toString()
                    
                    if (hasChanges) {
                        echo "✅ Staging: Changes detected - will apply"
                    } else {
                        echo "ℹ️ Staging: No changes detected - skipping apply"
                    }
                }
            }
        }
        
        stage('Staging: Apply') {
            when {
                expression { env.STAGING_HAS_CHANGES == 'true' }
            }
            steps {
                script {
                    echo "============================================"
                    echo "  Applying Staging Infrastructure"
                    echo "============================================"
                    
                    utils.sendDiscordNotification(
                        env.DISCORD_WEBHOOK_URL,
                        'STARTED',
                        'staging',
                        'apply',
                        'all',
                        env.BUILD_URL,
                        env.BUILD_NUMBER,
                        'Applying staging infrastructure changes'
                    )
                    
                    utils.terragruntApply('staging', 'all')
                    utils.terragruntOutput('staging')
                    
                    echo "✅ Staging infrastructure applied successfully"
                }
            }
        }
        
        stage('Staging: Smoke Test') {
            when {
                expression { env.STAGING_HAS_CHANGES == 'true' }
            }
            steps {
                script {
                    echo "Running smoke tests for staging..."
                    sh '''
                        # Wait for resources to stabilize
                        sleep 15
                        # Add your smoke tests here
                        echo "Smoke tests completed"
                    '''
                }
            }
        }
        
        // ==================== PRODUCTION ====================
        stage('Production: Validate') {
            steps {
                script {
                    utils.validateHcl('production')
                }
            }
        }
        
        stage('Production: Plan') {
            environment {
                // Override with production-specific credentials
                TF_VAR_unique_suffix = credentials('tf-unique-suffix-prod')
                TF_VAR_db_admin_username = credentials('db-admin-username-prod')
                TF_VAR_db_admin_password = credentials('db-admin-password-prod')
                TF_VAR_splunk_ssh_public_key = credentials('splunk-ssh-public-key-prod')
                TF_VAR_docker_image = credentials('docker-image-production')
                TF_VAR_admin_ip_range = credentials('admin-ip-range-prod')
            }
            steps {
                script {
                    echo "============================================"
                    echo "  Planning Production Infrastructure"
                    echo "============================================"
                    
                    def hasChanges = utils.terragruntPlan('production', 'all')
                    env.PRODUCTION_HAS_CHANGES = hasChanges.toString()
                    
                    if (hasChanges) {
                        echo "⚠️ Production: Changes detected - will require approval"
                    } else {
                        echo "ℹ️ Production: No changes detected - skipping apply"
                    }
                }
            }
        }
        
        stage('Production: Approval') {
            when {
                expression { env.PRODUCTION_HAS_CHANGES == 'true' }
            }
            steps {
                script {
                    utils.sendDiscordNotification(
                        env.DISCORD_WEBHOOK_URL,
                        'APPROVAL_REQUIRED',
                        'production',
                        'apply',
                        'all',
                        env.BUILD_URL,
                        env.BUILD_NUMBER,
                        '🚨 Production changes detected - manual approval required!'
                    )
                    
                    timeout(time: 60, unit: 'MINUTES') {
                        input(
                            message: '''⚠️ PRODUCTION DEPLOYMENT

Changes have been detected in the production infrastructure plan.
Review the plan output above carefully before proceeding.

Do you approve this deployment to PRODUCTION?''',
                            ok: 'Deploy to Production',
                            submitterParameter: 'APPROVER'
                        )
                    }
                    
                    echo "Production deployment approved by: ${env.APPROVER}"
                }
            }
        }
        
        stage('Production: Apply') {
            when {
                expression { env.PRODUCTION_HAS_CHANGES == 'true' }
            }
            environment {
                // Override with production-specific credentials
                TF_VAR_unique_suffix = credentials('tf-unique-suffix-prod')
                TF_VAR_db_admin_username = credentials('db-admin-username-prod')
                TF_VAR_db_admin_password = credentials('db-admin-password-prod')
                TF_VAR_splunk_ssh_public_key = credentials('splunk-ssh-public-key-prod')
                TF_VAR_docker_image = credentials('docker-image-production')
                TF_VAR_admin_ip_range = credentials('admin-ip-range-prod')
            }
            steps {
                script {
                    echo "============================================"
                    echo "  Applying Production Infrastructure"
                    echo "============================================"
                    
                    utils.sendDiscordNotification(
                        env.DISCORD_WEBHOOK_URL,
                        'STARTED',
                        'production',
                        'apply',
                        'all',
                        env.BUILD_URL,
                        env.BUILD_NUMBER,
                        "Applying production infrastructure changes (approved by: ${env.APPROVER})"
                    )
                    
                    utils.terragruntApply('production', 'all')
                    utils.terragruntOutput('production')
                    
                    echo "✅ Production infrastructure applied successfully"
                }
            }
        }
        
        stage('Production: Health Check') {
            when {
                expression { env.PRODUCTION_HAS_CHANGES == 'true' }
            }
            steps {
                script {
                    echo "Running health checks for production..."
                    sh '''
                        # Wait for resources to stabilize
                        sleep 30
                        # Add your health checks here
                        echo "Health checks completed"
                    '''
                }
            }
        }
        
        // ==================== SUMMARY ====================
        stage('Summary') {
            steps {
                script {
                    echo "============================================"
                    echo "  Pipeline Summary"
                    echo "============================================"
                    echo "Staging changes applied: ${env.STAGING_HAS_CHANGES}"
                    echo "Production changes applied: ${env.PRODUCTION_HAS_CHANGES}"
                    echo "============================================"
                }
            }
        }
    }
    
    post {
        always {
            script {
                utils.azureLogout()
                utils.cleanup()
            }
            cleanWs()
        }
        
        success {
            script {
                def summary = "Staging: ${env.STAGING_HAS_CHANGES == 'true' ? 'Applied' : 'No changes'} | Production: ${env.PRODUCTION_HAS_CHANGES == 'true' ? 'Applied' : 'No changes'}"
                
                echo "✅ Pipeline completed successfully!"
                utils.sendDiscordNotification(
                    env.DISCORD_WEBHOOK_URL,
                    'SUCCESS',
                    'all',
                    'apply',
                    'all',
                    env.BUILD_URL,
                    env.BUILD_NUMBER,
                    summary
                )
            }
        }
        
        failure {
            script {
                echo "❌ Pipeline failed!"
                utils.sendDiscordNotification(
                    env.DISCORD_WEBHOOK_URL,
                    'FAILURE',
                    'all',
                    'apply',
                    'all',
                    env.BUILD_URL,
                    env.BUILD_NUMBER,
                    'Infrastructure pipeline failed - Jira ticket created'
                )
                
                // Create Jira ticket
                utils.createJiraTicket(
                    env.JIRA_URL,
                    env.JIRA_USER,
                    env.JIRA_API_TOKEN,
                    env.JIRA_PROJECT_KEY,
                    'all',
                    'apply',
                    'all',
                    env.BUILD_URL,
                    ''
                )
            }
        }
        
        aborted {
            script {
                echo "⚠️ Pipeline was aborted."
                utils.sendDiscordNotification(
                    env.DISCORD_WEBHOOK_URL,
                    'ABORTED',
                    'all',
                    'apply',
                    'all',
                    env.BUILD_URL,
                    env.BUILD_NUMBER,
                    'Pipeline was manually aborted'
                )
            }
        }
    }
}
