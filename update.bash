#!/usr/bin/env bash

git config --global credential.helper store
echo "https://${GH_USERNAME}:${GH_TOKEN}@github.com" > $HOME/.git-credentials

# If the GitHub repo is not already cloned (it should be cached between builds), clone it now
if [ ! -f "$FULL_CLONE_DIR/.git/config" ]; then
  git clone "https://github.com/$TRAVIS_REPO_SLUG.git" "$FULL_CLONE_DIR"
  cat << 'EOF' >> "$FULL_CLONE_DIR/.git/config"
[svn-remote "svn"]
  url = https://josm.openstreetmap.de/svn
  fetch = trunk:refs/remotes/svn/trunk
  branches = branch/*:refs/remotes/svn/*
  tags = release/*:refs/remotes/svn/tags/*
EOF
fi

cd "$FULL_CLONE_DIR"

# Create the remote tracking branches for git-svn
originSvnBranches=`git branch -r --no-color --list "origin/svn/*"`
readarray -t originSvnBranchLines <<<"$originSvnBranches"
for originSvnBranch in "${originSvnBranchLines[@]}"; do
  # Trim the string and throw away the `origin/` prefix
  originSvnBranch=`echo "$originSvnBranch" | xargs echo | cut -c8-`

  # Create a remote tracking branch at the latest commit from the SVN branch that is in the GitHub repo
  mkdir -p $(dirname ".git/refs/remotes/$originSvnBranch")
  git rev-parse "origin/$originSvnBranch" > ".git/refs/remotes/$originSvnBranch"
done

git svn fetch --quiet

# Push all SVN branches to the GitHub repository
svnBranches=`git branch -r --no-color --list "svn/*"`
readarray -t lines <<<"$svnBranches"
for svnBranch in "${lines[@]}"; do
  # Trim the string
  svnBranch=`echo "$svnBranch" | xargs echo`
  # Push the branch to the GitHub repo
  git push origin "refs/remotes/$svnBranch:refs/heads/$svnBranch"
done
