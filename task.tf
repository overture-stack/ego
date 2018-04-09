data "template_file" "task_definition" {
  template = "${file("definitions/container-definition.json")}"
  vars {
    log_group_region         = "${var.region}"
    log_group_name = "${aws_cloudwatch_log_group.app.name}"
    image = "${var.image}"
    environment = "${var.environment}"
    service_name = "${var.service_name}"
    task_role_arn = "${var.task_role_arn}"
    vault_role = "${var.vault_role}"
    vault_url     = "${var.vault_url}"
    db_secret_path = "${var.db_secret_path}"
    ego_db_host     = "${var.ego_db_host}"
    ego_db_port 	= "${var.ego_db_port}"
    ego_server_port 	= "${var.ego_server_port}"
    ego_db 	= "${var.ego_db}"
    ego_iam_role 	= "${var.ego_iam_role}"
    ego_vault_port 	= "${var.ego_vault_port}"
    ego_vault_host 	= "${var.ego_vault_host}"
    ego_vault_uri 	= "${var.ego_vault_uri}"
    ego_vault_scheme 	= "${var.ego_vault_scheme}"
    ego_active_profiles = "${var.ego_active_profiles}"
    vault_application_name = "${var.vault_application_name}"
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name = "apps-${var.environment}/${var.application}"
}

resource "aws_ecs_task_definition" "kf-application-task" {
  family                = "${var.service_name}"
  task_role_arn = "${var.task_role_arn}"
  container_definitions = "${data.template_file.task_definition.rendered}"
  execution_role_arn    = "arn:aws:iam::538745987955:role/ecsTaskExecutionRole"
  network_mode = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu = "0.5vCPU"
  memory = "1024"
}
