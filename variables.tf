variable "service_name" {
  default = "default"
}
variable "environment" {
}
variable "application" {
  default = "not_set"
}
variable "organization" {
  default="kf"
}
variable "region" {
  default="us-east-1"
}
variable "chop_cidr" {}
variable "bucket" {}
variable "owner" {}
variable "organzation" {}
variable "image" {
}
variable "task_role_arn" {
}
variable "vault_url" {}
variable "db_secret_path" {
  default=""
}
variable "pg_db_name" {
  default = ""
}
variable "vault_role" {}
variable "ego_active_profiles" {}
variable "ego_db" {}
variable "ego_db_host" {}
variable "ego_db_port" {}
variable "ego_server_port" {}
variable "ego_iam_role" {}
variable "ego_vault_port" {}
variable "ego_vault_host" {}
variable "ego_vault_scheme" {}
variable "vault_application_name" {}
