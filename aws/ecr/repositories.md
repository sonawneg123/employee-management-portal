# AWS ECR Repositories — Employee Management Portal

## Bootstrap (manual — once per AWS account)

These two repositories must be created **once manually** (or via the bootstrap
script below).  They are intentionally NOT managed by Terraform in this phase.

### Repository 1 — Backend

| Setting               | Value                                     |
|-----------------------|-------------------------------------------|
| Repository name       | `employee-management-backend`             |
| Registry type         | Private                                   |
| Tag mutability        | **IMMUTABLE**                             |
| Scan on push          | Enabled (basic scanning)                  |
| KMS encryption        | AES-256 (default)                         |
| Lifecycle policy      | Keep last 20 images (see below)           |

### Repository 2 — Frontend

| Setting               | Value                                     |
|-----------------------|-------------------------------------------|
| Repository name       | `employee-management-frontend`            |
| Registry type         | Private                                   |
| Tag mutability        | **IMMUTABLE**                             |
| Scan on push          | Enabled (basic scanning)                  |
| KMS encryption        | AES-256 (default)                         |
| Lifecycle policy      | Keep last 20 images (see below)           |

---

## Tagging strategy

| Tag              | When applied     | Mutable? | Purpose                          |
|------------------|------------------|----------|----------------------------------|
| `<git-sha>`      | Every main push  | No       | Immutable deployment identifier  |
| `main-<sha>`     | Every main push  | No       | Human-readable branch prefix     |

`latest` is deliberately **never pushed**.  Every deployment references an
explicit SHA tag so rollback is trivially a one-variable change.

ECR full image references:
```
<account_id>.dkr.ecr.<region>.amazonaws.com/employee-management-backend:<sha>
<account_id>.dkr.ecr.<region>.amazonaws.com/employee-management-frontend:<sha>
```

---

## Lifecycle policy (apply to both repositories)

Keeps the 20 most recent tagged images and expires everything older.
Paste the JSON below into ECR → Lifecycle policies → Edit.

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 20 tagged images",
      "selection": {
        "tagStatus": "tagged",
        "tagPrefixList": ["main-"],
        "countType": "imageCountMoreThan",
        "countNumber": 20
      },
      "action": { "type": "expire" }
    },
    {
      "rulePriority": 2,
      "description": "Expire untagged images after 1 day",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 1
      },
      "action": { "type": "expire" }
    }
  ]
}
```

---

## Future Terraform scope

Once infrastructure-as-code is adopted, move:
- `aws_ecr_repository` (both repositories)
- `aws_ecr_lifecycle_policy` (both repositories)
- `aws_ecr_repository_policy` (lock down pull to EC2 role ARN only)
