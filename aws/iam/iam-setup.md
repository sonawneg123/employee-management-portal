# IAM Setup — Employee Management Portal

All IAM resources below are **manual bootstrap** steps.  
They should be migrated to Terraform in a future phase.

---

## 1. GitHub Actions OIDC Identity Provider

Create the OIDC provider once per AWS account.

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

> The thumbprint is GitHub's published value.  
> Verify at: https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services

---

## 2. GitHub Actions IAM Role (ECR push)

### 2a. Create the role with the OIDC trust policy

```bash
# Replace YOUR_ACCOUNT_ID, YOUR_GITHUB_ORG_OR_USER, YOUR_REPO_NAME
aws iam create-role \
  --role-name GitHubActions-ECRPush-Role \
  --assume-role-policy-document file://aws/iam/github-oidc-trust-policy.json \
  --description "Assumed by GitHub Actions via OIDC to push images to ECR"
```

### 2b. Create the ECR push permission policy

```bash
aws iam create-policy \
  --policy-name GitHubActions-ECRPush-Policy \
  --policy-document file://aws/iam/github-actions-ecr-push-policy.json \
  --description "Least-privilege ECR push permissions for GitHub Actions"
```

### 2c. Attach the policy to the role

```bash
# Replace YOUR_ACCOUNT_ID
aws iam attach-role-policy \
  --role-name GitHubActions-ECRPush-Role \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/GitHubActions-ECRPush-Policy
```

### 2d. Record the Role ARN

```bash
aws iam get-role --role-name GitHubActions-ECRPush-Role \
  --query 'Role.Arn' --output text
# → arn:aws:iam::YOUR_ACCOUNT_ID:role/GitHubActions-ECRPush-Role
```

Store this ARN in a GitHub Actions variable (not a secret — it is not sensitive):
- Repository → Settings → Secrets and variables → Actions → Variables
- Name: `AWS_ROLE_ARN`
- Value: `arn:aws:iam::YOUR_ACCOUNT_ID:role/GitHubActions-ECRPush-Role`

---

## 3. EC2 Instance Role (ECR pull)

### 3a. EC2 trust policy (allow EC2 service to assume the role)

```bash
aws iam create-role \
  --role-name EC2-EmpPortal-InstanceRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": { "Service": "ec2.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }]
  }' \
  --description "EC2 instance role for employee-management-portal — ECR pull only"
```

### 3b. Create the ECR pull permission policy

```bash
aws iam create-policy \
  --policy-name EC2-EmpPortal-ECRPull-Policy \
  --policy-document file://aws/iam/ec2-ecr-pull-policy.json \
  --description "Least-privilege ECR pull permissions for EC2 instance"
```

### 3c. Attach the policy

```bash
aws iam attach-role-policy \
  --role-name EC2-EmpPortal-InstanceRole \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/EC2-EmpPortal-ECRPull-Policy
```

### 3d. Create the instance profile and add the role

```bash
aws iam create-instance-profile \
  --instance-profile-name EC2-EmpPortal-InstanceProfile

aws iam add-role-to-instance-profile \
  --instance-profile-name EC2-EmpPortal-InstanceProfile \
  --role-name EC2-EmpPortal-InstanceRole
```

### 3e. Attach the profile when launching EC2

```bash
aws ec2 run-instances \
  ... \
  --iam-instance-profile Name=EC2-EmpPortal-InstanceProfile \
  ...
```

---

## 4. GitHub Actions Secrets / Variables

Set in: Repository → Settings → Secrets and variables → Actions

| Type     | Name               | Value                                                             |
|----------|--------------------|-------------------------------------------------------------------|
| Variable | `AWS_ROLE_ARN`     | `arn:aws:iam::ACCOUNT:role/GitHubActions-ECRPush-Role`           |
| Variable | `AWS_REGION`       | e.g. `us-east-1`                                                  |
| Variable | `ECR_REGISTRY`     | `ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com`                        |
| Secret   | `EC2_HOST`         | EC2 public IP or DNS name                                         |
| Secret   | `EC2_SSH_KEY`      | Private SSH key (PEM) for deployment user                         |
| Variable | `EC2_USER`         | `ec2-user` (Amazon Linux 2023 default)                            |

No `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` are used or stored.

---

## 5. Future Terraform scope

```
aws_iam_openid_connect_provider
aws_iam_role (GitHubActions-ECRPush-Role)
aws_iam_policy (GitHubActions-ECRPush-Policy)
aws_iam_role_policy_attachment (GitHub → ECR push)
aws_iam_role (EC2-EmpPortal-InstanceRole)
aws_iam_policy (EC2-EmpPortal-ECRPull-Policy)
aws_iam_role_policy_attachment (EC2 → ECR pull)
aws_iam_instance_profile (EC2-EmpPortal-InstanceProfile)
```
