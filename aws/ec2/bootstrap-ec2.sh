#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  aws/ec2/bootstrap-ec2.sh
#
#  EC2 user-data bootstrap script for Amazon Linux 2023.
#  Run once at instance launch (via --user-data) or manually as root.
#
#  Installs:
#    - Docker (latest from Amazon Linux extras)
#    - Docker Compose v2 (plugin, latest stable)
#    - AWS CLI v2 (already present on Amazon Linux 2023 — verified only)
#    - Creates a dedicated deployment user 'deploy' in the docker group
#
#  Does NOT install:
#    - Any application code
#    - Any AWS credentials (EC2 uses an IAM instance role)
#    - Any secrets
#
#  After launch:
#    1. Attach IAM instance profile EC2-EmpPortal-InstanceProfile to the instance
#    2. Add the deploy user's public SSH key to /home/deploy/.ssh/authorized_keys
#    3. Copy docker-compose.prod.yml and .env.production to /opt/emp-portal/
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

echo "==> Updating system packages"
dnf update -y

echo "==> Installing Docker"
dnf install -y docker
systemctl enable --now docker

echo "==> Installing Docker Compose v2 plugin"
COMPOSE_VERSION="v2.27.1"
COMPOSE_DIR="/usr/local/lib/docker/cli-plugins"
mkdir -p "${COMPOSE_DIR}"
curl -fsSL \
  "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64" \
  -o "${COMPOSE_DIR}/docker-compose"
chmod +x "${COMPOSE_DIR}/docker-compose"
docker compose version

echo "==> Verifying AWS CLI"
aws --version

echo "==> Creating deployment user"
# The 'deploy' user runs docker compose on behalf of GitHub Actions (SSH)
useradd -m -s /bin/bash deploy
usermod -aG docker deploy

# Prepare SSH directory (public key must be added manually after launch)
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys

echo "==> Creating application directory"
install -d -m 755 -o deploy -g deploy /opt/emp-portal

echo ""
echo "Bootstrap complete."
echo ""
echo "Next steps:"
echo "  1. Add deploy user's public key to /home/deploy/.ssh/authorized_keys"
echo "  2. Attach IAM instance profile EC2-EmpPortal-InstanceProfile"
echo "  3. Copy docker-compose.prod.yml and .env.production to /opt/emp-portal/"
echo "  4. Run: sudo -u deploy bash /opt/emp-portal/scripts/deploy.sh <SHA>"
