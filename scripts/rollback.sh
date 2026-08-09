#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  scripts/rollback.sh
#
#  Rolls back to a specified previous Git SHA.
#  Uses immutable ECR image tags — the previous image is always available.
#
#  Usage:
#    bash /opt/emp-portal/scripts/rollback.sh <previous-git-sha>
#
#  Example:
#    bash /opt/emp-portal/scripts/rollback.sh a1b2c3d4
#
#  How to find previous SHAs:
#    # List images in ECR (sorted by push date)
#    aws ecr describe-images \
#      --repository-name employee-management-backend \
#      --query 'sort_by(imageDetails, &imagePushedAt)[-10:].imageTags' \
#      --output table
#
#    # Or check the GitHub Actions run history for the previous successful deploy
#
#  The rollback is identical to a forward deploy — just a different IMAGE_TAG.
#  No database rollback is performed here; schema changes require a separate
#  Flyway-compatible down-migration strategy.
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

ROLLBACK_SHA="${1:?Usage: rollback.sh <previous-git-sha>}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Rolling back to: ${ROLLBACK_SHA}"
echo "    This will pull and start images tagged ${ROLLBACK_SHA} from ECR."
echo ""

# Delegate to the standard deploy script with the previous SHA
bash "${SCRIPT_DIR}/deploy.sh" "${ROLLBACK_SHA}"
