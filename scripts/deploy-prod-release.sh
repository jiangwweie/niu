#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/deploy-prod-release.sh [--restart] [--dry-run]

Builds backend/admin-web locally, uploads a versioned release to the production
server, writes REVISION, and switches /opt/niu/current to the new release.

Environment:
  NIU_SSH_HOST       SSH host or IP. Default: niu-prod
  NIU_SSH_USER       SSH user. Default: ubuntu
  NIU_SSH_KEY        SSH private key path. Optional
  NIU_SERVER_BASE    Server base directory. Default: /opt/niu
  NIU_JAVA_HOME      Java 17 home for backend build. Optional
  NIU_MAVEN_CMD      Maven command. Default: mvn
  NIU_NPM_CMD        npm command. Default: npm

Flags:
  --restart          Restart niu-backend after switching current.
  --dry-run          Print planned commands without building or uploading.
  -h, --help         Show this help.
USAGE
}

restart_backend=0
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
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

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

ssh_host="${NIU_SSH_HOST:-niu-prod}"
ssh_user="${NIU_SSH_USER:-ubuntu}"
ssh_key="${NIU_SSH_KEY:-}"
server_base="${NIU_SERVER_BASE:-/opt/niu}"
maven_cmd="${NIU_MAVEN_CMD:-mvn}"
npm_cmd="${NIU_NPM_CMD:-npm}"
java_home="${NIU_JAVA_HOME:-}"

if [[ -z "$java_home" && -d "/opt/homebrew/opt/openjdk@17" ]]; then
  java_home="/opt/homebrew/opt/openjdk@17"
fi

ssh_target="${ssh_user}@${ssh_host}"
ssh_opts=()
if [[ -n "$ssh_key" ]]; then
  ssh_opts=(-i "$ssh_key")
fi

commit_hash="$(git rev-parse --short=12 HEAD)"
commit_full="$(git rev-parse HEAD)"
release_id="$(date +%Y%m%d%H%M%S)-${commit_hash}"
release_dir="${server_base}/releases/${release_id}"
backend_jar="backend/target/xiaoniu-aftermarket-backend-0.0.1-SNAPSHOT.jar"

run() {
  echo "+ $*"
  if [[ "$dry_run" -eq 0 ]]; then
    "$@"
  fi
}

run_remote() {
  local remote_cmd="$1"
  echo "+ ssh ${ssh_opts[*]:-} ${ssh_target} ${remote_cmd}"
  if [[ "$dry_run" -eq 0 ]]; then
    if [[ ${#ssh_opts[@]} -gt 0 ]]; then
      ssh "${ssh_opts[@]}" "$ssh_target" "$remote_cmd"
    else
      ssh "$ssh_target" "$remote_cmd"
    fi
  fi
}

echo "Repository: $repo_root"
echo "Commit: $commit_full"
echo "Release: $release_id"
echo "Target: ${ssh_target}:${release_dir}"
echo
echo "Git status:"
git status --short --branch
echo

if [[ -n "$java_home" ]]; then
  export JAVA_HOME="$java_home"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "JAVA_HOME: $JAVA_HOME"
fi

if [[ "$dry_run" -eq 1 ]]; then
  echo "Dry run: build, upload, symlink switch, and restart are not executed."
else
  echo "Building backend jar with Java/Maven from current shell environment."
fi

run "$maven_cmd" -f backend/pom.xml package
run "$npm_cmd" --prefix admin-web run build

if [[ "$dry_run" -eq 0 ]]; then
  if [[ ! -f "$backend_jar" ]]; then
    echo "Backend jar not found: $backend_jar" >&2
    exit 1
  fi
  if [[ ! -f "admin-web/dist/index.html" ]]; then
    echo "admin-web dist not found: admin-web/dist/index.html" >&2
    exit 1
  fi
fi

remote_prepare=$(cat <<EOF
set -euo pipefail
mkdir -p '${release_dir}/backend' '${release_dir}/admin-web/dist'
EOF
)
run_remote "$remote_prepare"

if [[ ${#ssh_opts[@]} -gt 0 ]]; then
  run scp "${ssh_opts[@]}" "$backend_jar" "${ssh_target}:${release_dir}/backend/app.jar"
else
  run scp "$backend_jar" "${ssh_target}:${release_dir}/backend/app.jar"
fi
run rsync -az -e "ssh ${ssh_opts[*]:-}" admin-web/dist/ "${ssh_target}:${release_dir}/admin-web/dist/"

revision_content=$(cat <<EOF
release_id=${release_id}
commit=${commit_full}
created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
source_branch=$(git rev-parse --abbrev-ref HEAD)
EOF
)

remote_finalize=$(cat <<EOF
set -euo pipefail
cat > '${release_dir}/REVISION' <<'REVISION_EOF'
${revision_content}
REVISION_EOF
ln -sfn '${release_dir}' '${server_base}/.current.new'
mv -Tf '${server_base}/.current.new' '${server_base}/current'
ls -l '${server_base}/current'
cat '${release_dir}/REVISION'
EOF
)
run_remote "$remote_finalize"

if [[ "$restart_backend" -eq 1 ]]; then
  run_remote "sudo systemctl restart niu-backend && systemctl --no-pager --full status niu-backend"
else
  echo "Backend restart skipped. Pass --restart to restart niu-backend after switching current."
fi

echo "Release deployment completed: $release_id"
