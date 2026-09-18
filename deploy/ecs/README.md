# AWS ECS/Fargate

This folder holds starter Fargate task definitions. They are ready to fill in with an ECR image URI, RDS Postgres, and SQS/MSK when you have an AWS account.

Until then the production-shaped local stack is:

```bash
docker compose up --build
```

Do not list “deployed to ECS/Fargate” on a resume until a cluster is actually running.
