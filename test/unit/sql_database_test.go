package test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestSqlDatabaseModule tests the sql-database module
func TestSqlDatabaseModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-sql-rg-%s", uniqueID)
	sqlServerName := fmt.Sprintf("testsql%s", uniqueID)
	databaseName := fmt.Sprintf("testdb%s", uniqueID)
	location := "eastus"

	// Create resource group
	rgOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",
		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            location,
			"tags":                map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, rgOptions)
	terraform.InitAndApply(t, rgOptions)

	// Create SQL Database
	sqlOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/sql-database",
		Vars: map[string]interface{}{
			"sql_server_name":             sqlServerName,
			"resource_group_name":         resourceGroupName,
			"location":                    location,
			"sql_version":                 "12.0",
			"admin_username":              "sqladmin",
			"admin_password":              "TestP@ssw0rd123!",
			"database_name":               databaseName,
			"sku_name":                    "Basic",
			"max_size_gb":                 2,
			"zone_redundant":              false,
			"auto_pause_delay_in_minutes": -1,
			"min_capacity":                0.5,
			"collation":                   "SQL_Latin1_General_CP1_CI_AS",
			"minimum_tls_version":         "1.2",
			"azuread_admin_username":      "",
			"azuread_admin_object_id":     "",
			"subnet_id":                   "",
			"allow_azure_services":        true,
			"firewall_rules":              map[string]map[string]string{},
			"backup_retention_days":       7,
			"backup_interval_hours":       12,
			"ltr_weekly_retention":        "P1W",
			"ltr_monthly_retention":       "P1M",
			"ltr_yearly_retention":        "P1Y",
			"ltr_week_of_year":            1,
			"tags":                        map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, sqlOptions)
	terraform.InitAndApply(t, sqlOptions)

	// Validate outputs
	serverID := terraform.Output(t, sqlOptions, "server_id")
	serverFQDN := terraform.Output(t, sqlOptions, "server_fqdn")
	databaseID := terraform.Output(t, sqlOptions, "database_id")
	outputDatabaseName := terraform.Output(t, sqlOptions, "database_name")
	connectionString := terraform.Output(t, sqlOptions, "connection_string")

	// Assertions
	assert.NotEmpty(t, serverID, "Server ID should not be empty")
	assert.NotEmpty(t, serverFQDN, "Server FQDN should not be empty")
	assert.Contains(t, serverFQDN, sqlServerName, "Server FQDN should contain server name")
	assert.NotEmpty(t, databaseID, "Database ID should not be empty")
	assert.Equal(t, databaseName, outputDatabaseName, "Database name should match")
	assert.NotEmpty(t, connectionString, "Connection string should not be empty")
}

// TestSqlDatabaseModuleWithFirewallRules tests SQL database with firewall rules
func TestSqlDatabaseModuleWithFirewallRules(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-sql-fw-rg-%s", uniqueID)
	sqlServerName := fmt.Sprintf("testsqlfw%s", uniqueID)
	databaseName := fmt.Sprintf("testdbfw%s", uniqueID)
	location := "eastus"

	// Create resource group
	rgOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",
		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            location,
			"tags":                map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, rgOptions)
	terraform.InitAndApply(t, rgOptions)

	// Create SQL Database with firewall rules
	sqlOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/sql-database",
		Vars: map[string]interface{}{
			"sql_server_name":             sqlServerName,
			"resource_group_name":         resourceGroupName,
			"location":                    location,
			"sql_version":                 "12.0",
			"admin_username":              "sqladmin",
			"admin_password":              "TestP@ssw0rd123!",
			"database_name":               databaseName,
			"sku_name":                    "Basic",
			"max_size_gb":                 2,
			"zone_redundant":              false,
			"auto_pause_delay_in_minutes": -1,
			"min_capacity":                0.5,
			"collation":                   "SQL_Latin1_General_CP1_CI_AS",
			"minimum_tls_version":         "1.2",
			"azuread_admin_username":      "",
			"azuread_admin_object_id":     "",
			"subnet_id":                   "",
			"allow_azure_services":        true,
			"firewall_rules": map[string]map[string]string{
				"TestRule": {
					"start_ip": "10.0.0.1",
					"end_ip":   "10.0.0.255",
				},
			},
			"backup_retention_days": 7,
			"backup_interval_hours": 12,
			"ltr_weekly_retention":  "P1W",
			"ltr_monthly_retention": "P1M",
			"ltr_yearly_retention":  "P1Y",
			"ltr_week_of_year":      1,
			"tags":                  map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, sqlOptions)
	terraform.InitAndApply(t, sqlOptions)

	serverID := terraform.Output(t, sqlOptions, "server_id")
	assert.NotEmpty(t, serverID, "Server ID should not be empty")
}
