# AWS Infrastructure Setup — Employee Management Portal

Manual bootstrap guide for all AWS resources.  
All resources are currently created manually.  Future phase: migrate to Terraform.

---

## 1. VPC and Networking

Use the **default VPC** for initial deployment (simplest setup).  
For production hardening, create a custom VPC — see section 6.

---

## 2. Security Groups

Create two security groups in the same VPC.

### 2a. Application security group (emp-portal-app-sg)

Attached to: EC2 instance

| Direction | Protocol | Port | Source / Destination | Purpose |
|---|---|---|---|---|
| Inbound | TCP | 22 | Your admin IP or bastion CIDR | SSH management |
| Inbound | TCP | 80 | 0.0.0.0/0 | HTTP application traffic |
| Outbound | All | All | 0.0.0.0/0 | Allow all outbound |

> If using HTTPS later, add inbound TCP 443.  
> **Do NOT** open port 8080 or 3306 publicly.

```bash
# Create app security group
APP_SG_ID=$(aws ec2 create-security-group \
  --group-name emp-portal-app-sg \
  --description "Employee Management Portal — App server" \
  --query 'GroupId' --output text)

# SSH access (replace YOUR_ADMIN_IP)
aws ec2 authorize-security-group-ingress \
  --group-id "${APP_SG_ID}" \
  --protocol tcp --port 22 --cidr YOUR_ADMIN_IP/32

# HTTP
aws ec2 authorize-security-group-ingress \
  --group-id "${APP_SG_ID}" \
  --protocol tcp --port 80 --cidr 0.0.0.0/0

echo "App SG: ${APP_SG_ID}"
```

### 2b. Database security group (emp-portal-db-sg)

Attached to: RDS instance

| Direction | Protocol | Port | Source | Purpose |
|---|---|---|---|---|
| Inbound | TCP | 3306 | emp-portal-app-sg | MySQL from EC2 only |
| Outbound | All | All | 0.0.0.0/0 | Allow all outbound |

```bash
# Create DB security group
DB_SG_ID=$(aws ec2 create-security-group \
  --group-name emp-portal-db-sg \
  --description "Employee Management Portal — RDS MySQL" \
  --query 'GroupId' --output text)

# Allow MySQL ONLY from the app security group (not from 0.0.0.0/0)
aws ec2 authorize-security-group-ingress \
  --group-id "${DB_SG_ID}" \
  --protocol tcp --port 3306 \
  --source-group "${APP_SG_ID}"

echo "DB SG: ${DB_SG_ID}"
```

---

## 3. EC2 Instance

```bash
# Find the latest Amazon Linux 2023 AMI in your region
AL2023_AMI=$(aws ssm get-parameter \
  --name "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64" \
  --query 'Parameter.Value' --output text)

echo "AMI: ${AL2023_AMI}"

# Create an EC2 key pair for SSH access
aws ec2 create-key-pair \
  --key-name emp-portal-deploy \
  --query 'KeyMaterial' --output text > emp-portal-deploy.pem
chmod 400 emp-portal-deploy.pem

# Launch instance
INSTANCE_ID=$(aws ec2 run-instances \
  --image-id "${AL2023_AMI}" \
  --instance-type t3.small \
  --key-name emp-portal-deploy \
  --security-group-ids "${APP_SG_ID}" \
  --iam-instance-profile Name=EC2-EmpPortal-InstanceProfile \
  --user-data file://aws/ec2/bootstrap-ec2.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=emp-portal-prod}]' \
  --query 'Instances[0].InstanceId' --output text)

echo "Instance ID: ${INSTANCE_ID}"

# Wait until running
aws ec2 wait instance-running --instance-ids "${INSTANCE_ID}"

# Get public IP
PUBLIC_IP=$(aws ec2 describe-instances \
  --instance-ids "${INSTANCE_ID}" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "Public IP: ${PUBLIC_IP}"
```

### After launch — add deploy user SSH key

```bash
# Generate a separate deploy keypair (for GitHub Actions — not the admin key)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ./deploy_key -N ""

# Copy the public key to the EC2 instance
ssh -i emp-portal-deploy.pem ec2-user@${PUBLIC_IP} \
  "sudo bash -c 'echo $(cat deploy_key.pub) >> /home/deploy/.ssh/authorized_keys'"

# Store the private key in GitHub Actions Secrets
# Name: EC2_SSH_KEY
# Value: contents of deploy_key (the private key)
echo ""
echo "Add to GitHub Secrets:"
echo "  EC2_SSH_KEY = $(cat deploy_key)"
echo "  EC2_HOST    = ${PUBLIC_IP}"
```

### Verify bootstrap

```bash
# SSH into the instance and run the verification script
ssh -i emp-portal-deploy.pem ec2-user@${PUBLIC_IP}

# On EC2:
AWS_REGION=us-east-1 bash /opt/emp-portal/scripts/verify-ec2.sh
# OR if scripts are not yet copied:
AWS_REGION=us-east-1 bash /tmp/verify-ec2.sh
```

---

## 4. RDS MySQL 8

### 4a. Create DB subnet group

```bash
# Get all subnet IDs in the default VPC
SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=default-for-az,Values=true" \
  --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')

aws rds create-db-subnet-group \
  --db-subnet-group-name emp-portal-subnet-group \
  --db-subnet-group-description "Employee Management Portal subnets" \
  --subnet-ids $(echo "${SUBNET_IDS}" | tr ',' ' ')
```

### 4b. Create RDS instance

```bash
# Generate a strong master password (store this securely — not in Git)
MASTER_PASSWORD="$(openssl rand -base64 24 | tr -d '/+=')"
echo "Master password: ${MASTER_PASSWORD}"
echo "(Store this securely — it is the RDS master user password)"

aws rds create-db-instance \
  --db-instance-identifier emp-portal-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version "8.0" \
  --master-username admin \
  --master-user-password "${MASTER_PASSWORD}" \
  --db-name emp_portal \
  --vpc-security-group-ids "${DB_SG_ID}" \
  --db-subnet-group-name emp-portal-subnet-group \
  --backup-retention-period 7 \
  --storage-encrypted \
  --allocated-storage 20 \
  --storage-type gp2 \
  --no-publicly-accessible \
  --deletion-protection \
  --tags 'Key=Name,Value=emp-portal-db'

echo "RDS creation initiated. Waiting for available state (~10 minutes)..."
aws rds wait db-instance-available --db-instance-identifier emp-portal-db

RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier emp-portal-db \
  --query 'DBInstances[0].Endpoint.Address' --output text)

echo "RDS Endpoint: ${RDS_ENDPOINT}"
```

### 4c. Create application database user

After RDS is available, connect using the master user and create a restricted application user.

```bash
# Connect to RDS from EC2 (requires mysql client)
# On EC2: sudo dnf install -y mariadb105

# From EC2:
mysql -h "${RDS_ENDPOINT}" -u admin -p"${MASTER_PASSWORD}" << 'SQL'
-- Create application user (NOT the master user)
CREATE USER 'emp_user'@'%' IDENTIFIED BY 'REPLACE_WITH_STRONG_APP_PASSWORD';

-- Grant only what is needed: the application database only
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, REFERENCES
  ON emp_portal.* TO 'emp_user'@'%';

FLUSH PRIVILEGES;

-- Verify
SELECT user, host FROM mysql.user WHERE user = 'emp_user';
SQL
```

### 4d. RDS configuration summary

| Setting | Value |
|---|---|
| Engine | MySQL 8.0 |
| Instance class | db.t3.micro (upgrade as needed) |
| Database name | `emp_portal` |
| Publicly accessible | **No** |
| Storage encryption | **Enabled** |
| Backup retention | 7 days |
| Deletion protection | **Enabled** |
| Multi-AZ | No (single-AZ for initial deployment — enable for production) |
| Security group | `emp-portal-db-sg` (port 3306 from `emp-portal-app-sg` only) |

---

## 5. Verify EC2 → RDS connectivity

From the EC2 instance:

```bash
# Install mysql client (if needed)
sudo dnf install -y mariadb105

# Test connection
mysql -h YOUR_RDS_ENDPOINT -u emp_user -p
# Enter app user password when prompted
# Should connect and show 'mysql>' prompt

# Quick verify:
mysql -h YOUR_RDS_ENDPOINT -u emp_user -p -e "SELECT VERSION();"
# Should return: 8.0.x
```

---

## 6. Production .env.production on EC2

After RDS is available:

```bash
# On EC2
sudo -u deploy bash
cd /opt/emp-portal

# Copy the template
cp /path/to/repo/.env.production.example /opt/emp-portal/.env.production

# Fill in values
# DB_HOST  = output of 'aws rds describe-db-instances ... Endpoint.Address'
# DB_USER  = emp_user
# DB_PASSWORD = the strong password created in step 4c
# JWT_SECRET  = $(openssl rand -base64 48)
# CORS_ORIGINS = http://YOUR_EC2_PUBLIC_IP

nano /opt/emp-portal/.env.production

# Lock permissions
chmod 600 /opt/emp-portal/.env.production
ls -la /opt/emp-portal/.env.production
# Should show: -rw------- 1 deploy deploy
```

---

## 7. GitHub Actions variables and secrets

Set these in: **Repository → Settings → Secrets and variables → Actions**

| Type | Name | Value | Where to find |
|---|---|---|---|
| Variable | `AWS_ROLE_ARN` | `arn:aws:iam::ACCOUNT:role/GitHubActions-ECRPush-Role` | `aws iam get-role --role-name GitHubActions-ECRPush-Role --query Role.Arn` |
| Variable | `AWS_REGION` | `us-east-1` | Your chosen region |
| Variable | `ECR_REGISTRY` | `ACCOUNT.dkr.ecr.REGION.amazonaws.com` | From ECR console or `bootstrap-ecr.sh` output |
| Variable | `EC2_USER` | `deploy` | Fixed (set by bootstrap-ec2.sh) |
| Secret | `EC2_HOST` | EC2 public IP or Elastic IP | EC2 console → Instances → your instance |
| Secret | `EC2_SSH_KEY` | Private key (PEM) for deploy user | `deploy_key` file generated in step 3 |

---

## 8. Future networking improvements (not in this phase)

- Elastic IP: assign to EC2 so the IP doesn't change on restart
- Custom domain: Route53 A record → Elastic IP
- HTTPS: AWS Certificate Manager + Application Load Balancer (or Certbot on EC2)
- Private VPC: move RDS to private subnets, NAT Gateway for EC2 outbound
- Multi-AZ RDS: enable when SLA requires it
