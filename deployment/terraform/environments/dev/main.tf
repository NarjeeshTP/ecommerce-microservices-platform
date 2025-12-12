terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  backend "s3" {
    bucket = "ecommerce-platform-terraform-state"
    key    = "dev/terraform.tfstate"
    region = "us-east-1"
    encrypt = true
    dynamodb_table = "terraform-state-lock"
  }
}
provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "ecommerce-platform"
      Environment = "dev"
      ManagedBy   = "terraform"
    }
  }
}
# VPC Module
module "networking" {
  source = "../../modules/networking"
  project             = var.project_name
  environment         = var.environment
  vpc_cidr            = var.vpc_cidr
  availability_zones  = var.availability_zones
  enable_nat_gateway  = true
  single_nat_gateway  = true  # Cost-saving for dev
}
# EKS Cluster Module
module "kubernetes" {
  source = "../../modules/kubernetes"
  project            = var.project_name
  environment        = var.environment
  cluster_version    = var.cluster_version
  vpc_id             = module.networking.vpc_id
  subnet_ids         = module.networking.private_subnet_ids
  node_groups = {
    general = {
      desired_size = var.node_count
      min_size     = var.node_min_count
      max_size     = var.node_max_count
      instance_types = [var.instance_type]
    }
  }
}
# RDS PostgreSQL Module
module "database" {
  source = "../../modules/database"
  project              = var.project_name
  environment          = var.environment
  vpc_id               = module.networking.vpc_id
  subnet_ids           = module.networking.private_subnet_ids
  instance_class       = var.db_instance_class
  allocated_storage    = var.db_allocated_storage
  database_name        = "catalogdb"
  master_username      = var.db_master_username
  backup_retention_days = 7
  skip_final_snapshot  = true  # For dev environment
}
# Outputs
output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.kubernetes.cluster_endpoint
}
output "cluster_name" {
  description = "EKS cluster name"
  value       = module.kubernetes.cluster_name
}
output "database_endpoint" {
  description = "RDS instance endpoint"
  value       = module.database.endpoint
  sensitive   = true
}
output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}
