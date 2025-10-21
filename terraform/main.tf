# Terraform configuration for GloboBank Fraud Detection Service
terraform {
  required_version = ">= 1.5"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.10"
    }
  }
  
  backend "s3" {
    bucket         = "globobank-terraform-state"
    key            = "fraud-service/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-lock-table"
  }
}

# AWS Provider Configuration
provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "GloboBank Fraud Detection"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Service     = "fraud-detection"
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# VPC Configuration
resource "aws_vpc" "fraud_vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name = "${var.project_name}-vpc"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "fraud_igw" {
  vpc_id = aws_vpc.fraud_vpc.id
  
  tags = {
    Name = "${var.project_name}-igw"
  }
}

# Private Subnets for Database
resource "aws_subnet" "fraud_private_subnets" {
  count = length(var.availability_zones)
  
  vpc_id            = aws_vpc.fraud_vpc.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "${var.project_name}-private-subnet-${count.index + 1}"
    Type = "Private"
  }
}

# Public Subnets for Load Balancer
resource "aws_subnet" "fraud_public_subnets" {
  count = length(var.availability_zones)
  
  vpc_id                  = aws_vpc.fraud_vpc.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
  
  tags = {
    Name = "${var.project_name}-public-subnet-${count.index + 1}"
    Type = "Public"
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "fraud_db_subnet_group" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.fraud_private_subnets[*].id
  
  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

# Security Group for RDS
resource "aws_security_group" "fraud_rds_sg" {
  name_prefix = "${var.project_name}-rds-sg"
  vpc_id      = aws_vpc.fraud_vpc.id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.fraud_app_sg.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}

# Security Group for Application
resource "aws_security_group" "fraud_app_sg" {
  name_prefix = "${var.project_name}-app-sg"
  vpc_id      = aws_vpc.fraud_vpc.id
  
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "${var.project_name}-app-sg"
  }
}

# RDS PostgreSQL Instance
resource "aws_db_instance" "fraud_postgres" {
  identifier                = "${var.project_name}-postgres"
  allocated_storage         = var.db_allocated_storage
  max_allocated_storage     = var.db_max_allocated_storage
  storage_type              = "gp3"
  storage_encrypted         = true
  
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class
  
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.fraud_rds_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.fraud_db_subnet_group.name
  
  backup_retention_period = var.db_backup_retention_period
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot       = var.environment == "dev"
  final_snapshot_identifier = var.environment == "dev" ? null : "${var.project_name}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"
  
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.fraud_rds_monitoring_role.arn
  
  performance_insights_enabled = true
  performance_insights_retention_period = 7
  
  tags = {
    Name = "${var.project_name}-postgres"
  }
}

# IAM Role for RDS Monitoring
resource "aws_iam_role" "fraud_rds_monitoring_role" {
  name = "${var.project_name}-rds-monitoring-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "fraud_rds_monitoring_policy" {
  role       = aws_iam_role.fraud_rds_monitoring_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# EKS Cluster (placeholder for future implementation)
resource "aws_eks_cluster" "fraud_eks_cluster" {
  count = var.create_eks_cluster ? 1 : 0
  
  name     = "${var.project_name}-eks-cluster"
  role_arn = aws_iam_role.fraud_eks_cluster_role[0].arn
  version  = var.eks_cluster_version
  
  vpc_config {
    subnet_ids              = concat(aws_subnet.fraud_private_subnets[*].id, aws_subnet.fraud_public_subnets[*].id)
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = var.eks_public_access_cidrs
  }
  
  encryption_config {
    provider {
      key_arn = aws_kms_key.fraud_eks_key[0].arn
    }
    resources = ["secrets"]
  }
  
  depends_on = [
    aws_iam_role_policy_attachment.fraud_eks_cluster_policy,
    aws_iam_role_policy_attachment.fraud_eks_vpc_resource_controller_policy,
  ]
  
  tags = {
    Name = "${var.project_name}-eks-cluster"
  }
}

# EKS Cluster IAM Role
resource "aws_iam_role" "fraud_eks_cluster_role" {
  count = var.create_eks_cluster ? 1 : 0
  
  name = "${var.project_name}-eks-cluster-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "fraud_eks_cluster_policy" {
  count = var.create_eks_cluster ? 1 : 0
  
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.fraud_eks_cluster_role[0].name
}

resource "aws_iam_role_policy_attachment" "fraud_eks_vpc_resource_controller_policy" {
  count = var.create_eks_cluster ? 1 : 0
  
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
  role       = aws_iam_role.fraud_eks_cluster_role[0].name
}

# KMS Key for EKS Encryption
resource "aws_kms_key" "fraud_eks_key" {
  count = var.create_eks_cluster ? 1 : 0
  
  description             = "KMS key for ${var.project_name} EKS cluster encryption"
  deletion_window_in_days = 7
  
  tags = {
    Name = "${var.project_name}-eks-key"
  }
}

resource "aws_kms_alias" "fraud_eks_key_alias" {
  count = var.create_eks_cluster ? 1 : 0
  
  name          = "alias/${var.project_name}-eks-key"
  target_key_id = aws_kms_key.fraud_eks_key[0].key_id
}