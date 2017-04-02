#!/usr/bin/env bash

currentDir=`pwd`
fullCloneDir=`readlink -f "$FULL_CLONE_DIR"`

git config --global credential.helper store
echo "https://${GH_USERNAME}:${GH_TOKEN}@github.com" > $HOME/.git-credentials

# If the GitHub repo is not already cloned (it should be cached between builds), clone it now
if [ ! -f "$fullCloneDir/.git/config" ]; then
  echo "Clone current state from GitHub…"
  git clone "https://github.com/$TRAVIS_REPO_SLUG.git" "$fullCloneDir"
  echo "Configure SVN remote…"
  cat << 'EOF' >> "$fullCloneDir/.git/config"
[svn-remote "svn"]
  url = https://josm.openstreetmap.de/svn
  fetch = trunk:refs/remotes/svn/trunk
  branches = branch/*:refs/remotes/svn/*
  tags = release/*:refs/remotes/svn/tags/*
EOF
else
  echo "Use clone in directory $fullCloneDir which is already there…"
  cd "$fullCloneDir"
  git fetch origin
fi

cd "$fullCloneDir"

# Create the remote tracking branches for git-svn
originSvnBranches=`git branch -r --no-color --list "origin/svn/*"`
readarray -t originSvnBranchLines <<<"$originSvnBranches"
for originSvnBranch in "${originSvnBranchLines[@]}"; do
  # Trim the string and throw away the `origin/` prefix
  originSvnBranch=`echo "$originSvnBranch" | xargs echo | cut -c8-`
  echo "Create remote tracking branch refs/remotes/$originSvnBranch…"
  # Create a remote tracking branch at the latest commit from the SVN branch that is in the GitHub repo
  mkdir -p $(dirname ".git/refs/remotes/$originSvnBranch")
  git rev-parse "origin/$originSvnBranch" > ".git/refs/remotes/$originSvnBranch"
done

echo "Fetch from SVN…"
git svn fetch --quiet

# Push all SVN branches to the GitHub repository
svnBranches=`git branch -r --no-color --list "svn/*"`
readarray -t lines <<<"$svnBranches"
for svnBranch in "${lines[@]}"; do
  # Trim the string
  svnBranch=`echo "$svnBranch" | xargs echo`
  echo "Push current state of branch $svnBranch to GitHub…"
  # Push the branch to the GitHub repo
  git push origin "refs/remotes/$svnBranch:refs/heads/$svnBranch"
done
