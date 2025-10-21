# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.fraud_vpc.id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = aws_vpc.fraud_vpc.cidr_block
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.fraud_private_subnets[*].id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.fraud_public_subnets[*].id
}

# Database Outputs
output "database_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.fraud_postgres.endpoint
  sensitive   = true
}

output "database_port" {
  description = "RDS instance port"
  value       = aws_db_instance.fraud_postgres.port
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.fraud_postgres.db_name
}

output "database_username" {
  description = "Database username"
  value       = aws_db_instance.fraud_postgres.username
  sensitive   = true
}

output "database_connection_string" {
  description = "Database connection string (without password)"
  value       = "jdbc:postgresql://${aws_db_instance.fraud_postgres.endpoint}/${aws_db_instance.fraud_postgres.db_name}"
  sensitive   = true
}

# Security Group Outputs
output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = aws_security_group.fraud_rds_sg.id
}

output "app_security_group_id" {
  description = "ID of the application security group"
  value       = aws_security_group.fraud_app_sg.id
}

# EKS Outputs (conditional)
output "eks_cluster_id" {
  description = "ID of the EKS cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].id : null
}

output "eks_cluster_arn" {
  description = "ARN of the EKS cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].arn : null
}

output "eks_cluster_endpoint" {
  description = "Endpoint for EKS control plane"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].endpoint : null
  sensitive   = true
}

output "eks_cluster_version" {
  description = "The Kubernetes server version for the EKS cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].version : null
}

output "eks_cluster_platform_version" {
  description = "Platform version for the EKS cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].platform_version : null
}

output "eks_cluster_status" {
  description = "Status of the EKS cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].status : null
}

output "eks_cluster_certificate_authority" {
  description = "Base64 encoded certificate data required to communicate with the cluster"
  value       = var.create_eks_cluster ? aws_eks_cluster.fraud_eks_cluster[0].certificate_authority[0].data : null
  sensitive   = true
}

# IAM Outputs
output "rds_monitoring_role_arn" {
  description = "ARN of the RDS monitoring role"
  value       = aws_iam_role.fraud_rds_monitoring_role.arn
}

output "eks_cluster_role_arn" {
  description = "ARN of the EKS cluster role"
  value       = var.create_eks_cluster ? aws_iam_role.fraud_eks_cluster_role[0].arn : null
}

# KMS Outputs
output "eks_kms_key_id" {
  description = "ID of the KMS key used for EKS encryption"
  value       = var.create_eks_cluster ? aws_kms_key.fraud_eks_key[0].key_id : null
}

output "eks_kms_key_arn" {
  description = "ARN of the KMS key used for EKS encryption"
  value       = var.create_eks_cluster ? aws_kms_key.fraud_eks_key[0].arn : null
}

# Environment Information
output "environment" {
  description = "Environment name"
  value       = var.environment
}

output "project_name" {
  description = "Project name"
  value       = var.project_name
}

output "aws_region" {
  description = "AWS region"
  value       = var.aws_region
}

output "aws_account_id" {
  description = "AWS account ID"
  value       = data.aws_caller_identity.current.account_id
}

# Application Configuration Outputs
output "application_url" {
  description = "URL where the fraud service will be accessible"
  value       = var.create_eks_cluster ? "https://fraud-api.globobank.com" : "http://localhost:8080/fraud/v1"
}

output "health_check_url" {
  description = "Health check endpoint URL"
  value       = var.create_eks_cluster ? "https://fraud-api.globobank.com/actuator/health" : "http://localhost:8080/fraud/v1/actuator/health"
}

# Configuration for application deployment
output "application_config" {
  description = "Configuration values for application deployment"
  value = {
    database_url      = "jdbc:postgresql://${aws_db_instance.fraud_postgres.endpoint}/${aws_db_instance.fraud_postgres.db_name}"
    database_username = aws_db_instance.fraud_postgres.username
    vpc_id           = aws_vpc.fraud_vpc.id
    private_subnets  = aws_subnet.fraud_private_subnets[*].id
    public_subnets   = aws_subnet.fraud_public_subnets[*].id
    security_groups = {
      app = aws_security_group.fraud_app_sg.id
      rds = aws_security_group.fraud_rds_sg.id
    }
  }
  sensitive = true
}