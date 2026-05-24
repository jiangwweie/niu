#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/rollback-prod-release.sh [--release RELEASE_ID] [--restart] [--dry-run]

Lists production releases and, when --release is provided, switches
/opt/niu/current to the specified release. Releases are never deleted.

Environment:
  NIU_SSH_HOST       SSH host or IP. Default: niu-prod
  NIU_SSH_USER       SSH user. Default: ubuntu
  NIU_SSH_KEY        SSH private key path. Optional
  NIU_SERVER_BASE    Server base directory. Default: /opt/niu

Flags:
  --release ID       Release id under /opt/niu/releases.
  --restart          Restart niu-backend after switching current.
  --dry-run          Print planned commands without changing current.
  -h, --help         Show this help.
USAGE
}

release_id=""
restart_backend=0
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --release)
      if [[ $# -lt 2 ]]; then
        echo "--release requires a value" >&2
        exit 2
      fi
      release_id="$2"
      shift
      ;;
    --restart)
      restart_backend=1
      ;;
    --dry-run)
      dry_run=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

ssh_host="${NIU_SSH_HOST:-niu-prod}"
ssh_user="${NIU_SSH_USER:-ubuntu}"
ssh_key="${NIU_SSH_KEY:-}"
server_base="${NIU_SERVER_BASE:-/opt/niu}"

ssh_target="${ssh_user}@${ssh_host}"
ssh_opts=()
if [[ -n "$ssh_key" ]]; then
  ssh_opts=(-i "$ssh_key")
fi

run_remote() {
  local remote_cmd="$1"
  echo "+ ssh ${ssh_opts[*]:-} ${ssh_target} ${remote_cmd}"
  if [[ "$dry_run" -eq 0 ]]; then
    ssh "${ssh_opts[@]}" "$ssh_target" "$remote_cmd"
  fi
}

list_cmd=$(cat <<EOF
set -euo pipefail
echo 'Available releases:'
find '${server_base}/releases' -mindepth 1 -maxdepth 1 -type d -printf '%f\\n' | sort
echo
echo 'Current:'
readlink '${server_base}/current' 2>/dev/null || true
EOF
)
run_remote "$list_cmd"

if [[ -z "$release_id" ]]; then
  echo "No --release provided; listed releases only."
  exit 0
fi

case "$release_id" in
  */*|.*|"")
    echo "Invalid release id: $release_id" >&2
    exit 2
    ;;
esac

target_release="${server_base}/releases/${release_id}"
switch_cmd=$(cat <<EOF
set -euo pipefail
test -d '${target_release}'
test -f '${target_release}/backend/app.jar'
test -f '${target_release}/admin-web/dist/index.html'
ln -sfn '${target_release}' '${server_base}/.current.new'
mv -Tf '${server_base}/.current.new' '${server_base}/current'
ls -l '${server_base}/current'
cat '${target_release}/REVISION' 2>/dev/null || true
EOF
)
run_remote "$switch_cmd"

if [[ "$restart_backend" -eq 1 ]]; then
  run_remote "sudo systemctl restart niu-backend && systemctl --no-pager --full status niu-backend"
else
  echo "Backend restart skipped. Pass --restart to restart niu-backend after switching current."
fi
