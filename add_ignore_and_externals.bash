#!/usr/bin/env bash

# This script updates the branch trunk with all the commits to refs/remotes/svn/trunk, that are not ancestors of the trunk branch.

# DISCARD_UNCOMMITTED_CHANGES denotes, if the script should discard changes, that are currently in the repo on top of the HEAD (either staged or unstaged).
if [ -z "$DISCARD_UNCOMMITTED_CHANGES" ]; then
  DISCARD_UNCOMMITTED_CHANGES=false
fi
echo "██ DISCARD_UNCOMMITTED_CHANGES = $DISCARD_UNCOMMITTED_CHANGES"
# RESET_TRUNK_TO_ORIGIN_STATE denotes, if the local trunk branch should be reset to the current refs/remotes/origin/trunk commit
if [ -z "$RESET_TRUNK_TO_ORIGIN_STATE" ]; then
  RESET_TRUNK_TO_ORIGIN_STATE=false
fi
echo "██ RESET_TRUNK_TO_ORIGIN_STATE = $RESET_TRUNK_TO_ORIGIN_STATE"
# PUSH_TO_ORIGIN denotes, if this script should push the end result (the updated trunk branch) to the remote called "origin"
if [ -z "$PUSH_TO_ORIGIN" ]; then
  PUSH_TO_ORIGIN=false
fi
echo "██ PUSH_TO_ORIGIN = $PUSH_TO_ORIGIN"
# FULL_CLONE_DIR is the directory where the local clone of the JOSM repository resides
# SVN_EXTERNALS_CLONE_DIR is the directory where the local clones of the SVN externals are stored
if [ -z "$FULL_CLONE_DIR" ] || [ -z "$SVN_EXTERNALS_CLONE_DIR" ]; then
  echo "██ Please set the variables FULL_CLONE_DIR and SVN_EXTERNALS_CLONE_DIR"
  exit 1
fi
FULL_CLONE_DIR=`readlink -f "$FULL_CLONE_DIR"`
echo "██ FULL_CLONE_DIR = $FULL_CLONE_DIR"
SVN_EXTERNALS_CLONE_DIR=`readlink -f "$SVN_EXTERNALS_CLONE_DIR"`
echo "██ SVN_EXTERNALS_CLONE_DIR = $SVN_EXTERNALS_CLONE_DIR"

cd "$FULL_CLONE_DIR"

if [ "$DISCARD_UNCOMMITTED_CHANGES" = false ] && [ $(git status --short -uno | wc -l) != 0 ]; then
  echo "██ You have uncommitted changes, aborting now."
  exit 1
fi

if [ "$RESET_TRUNK_TO_ORIGIN_STATE" = true ]; then
  git remote prune origin
  git fetch origin trunk
  git reset --hard refs/remotes/origin/trunk
  git checkout -B trunk refs/remotes/origin/trunk
fi
git rev-parse --verify trunk
if [ $? != 0 ]; then
  echo "██ Branch trunk does not exist, aborting now."
  exit 1
fi
git checkout trunk
if [ $? != 0 ]; then
  echo "██ Could not checkout branch trunk."
  exit 1
fi

# The first 20 of the revisions that are on the svn/trunk branch but not on the trunk branch
newRevisions=`git rev-list --reverse trunk..refs/remotes/svn/trunk | head -20`
git merge-base --is-ancestor trunk refs/remotes/svn/trunk
if [ $? == 0 ]; then
  newRevisions=`git rev-list -1 trunk`$'\n'"$newRevisions"
fi

while read -r newRevision; do
  commitDate=`git log -1 --format="%ai" "$newRevision"`
  commitName=`git log -1 --format="%an" "$newRevision"`
  commitEmail=`git log -1 --format="%ae" "$newRevision"`
  svnRevisionNumber=`git svn find-rev "$newRevision"`

  if [ $? != 0 ]; then
    echo "██ Could not get the SVN revision number for $newRevision. Aborting."
    exit 1
  else
    printf "████\n██ Analyzing r$svnRevisionNumber ($newRevision)\n████\n"

    # Create the new commit without making any changes (it will be amended later to contain the changes)
    git merge-base --is-ancestor HEAD $newRevision
    if [ $? == 0 ]; then
      # Branch off the svn/trunk branch if we are on it
      GIT_AUTHOR_DATE="$commitDate" GIT_AUTHOR_NAME="$commitName" GIT_AUTHOR_EMAIL="$commitEmail" GIT_COMMITTER_NAME="$commitName" GIT_COMMITTER_EMAIL="$commitEmail" git commit -m "WIP" --allow-empty
    else
      # Merge the new commit from the svn/trunk branch otherwise
      GIT_AUTHOR_DATE="$commitDate" GIT_AUTHOR_NAME="$commitName" GIT_AUTHOR_EMAIL="$commitEmail" GIT_COMMITTER_NAME="$commitName" GIT_COMMITTER_EMAIL="$commitEmail" git merge $newRevision -s ours -m "WIP"
    fi
    if [ $? != 0 ]; then
      echo "██ Could not commit the base commit. Aborting now."
      exit 1
    fi

    # Create the .gitignore file(s)
    git checkout -q "$newRevision"
    git svn create-ignore --revision "$svnRevisionNumber"
    if [ $? != 0 ]; then
      echo "██ Could not generate .gitignore file(s) from svn:ignore."
      exit 1
    fi
    echo "██ Created .gitignore(s)"

    commitMessage="[r$svnRevisionNumber] Add svn:ignore and svn:externals"$'\n\n'`git status -uno --short`$'\n'

    externals=`git svn show-externals --revision "$svnRevisionNumber"`
    if [ $? != 0 ]; then
      echo "██ Failed to get the externals for revision $svnRevisionNumber."
      exit 1
    fi
    # Normalize externals (separate path and URL and put URL second)
    externals=`printf "$externals" | grep "^[^#]" | sed -re "s~([[:graph:]]+)(https?\://[[:graph:]]+)[[:space:]]*([[:graph:]]+)~\1\3 \2~g"`

    if [ `printf "$externals" | wc -c` == 0 ]; then
      echo "██ Revision $svnRevisionNumber contains no externals."
    else
      # Iterate all externals
      while read -r external; do
        external=" $external "
        externalPath=
        externalRevision=
        externalURL=
        externalURLSha=
        externalUUID=

        revisionRegex="[[:space:]]-r[[:space:]]*([[:digit:]]+)[[:space:]]"
        if [[ $external =~ $revisionRegex ]]; then
          externalRevision="${BASH_REMATCH[1]}"
        fi
        external=`echo -n "$external" | sed -re "s~$revisionRegex~ ~"`

        urlRegex="(https?://[[:graph:]]+)[[:space:]]+"
        if [[ $external =~ $urlRegex ]]; then
          externalURL="${BASH_REMATCH[1]}"
          if [[ $externalURL =~ "(.*)@([[:digit:]]+)$" ]]; then
            # Special case where the version is given as @1234 at the end of the URL
            externalURL="${BASH_REMATCH[1]}"
            externalRevision="${BASH_REMATCH[2]}"
          fi
          # URL where on the web you can find the SVN repo that is used as an external (can be a path to a subdirectory of that repo)
          # If the specified URL is redirected, resolves to the final URL (see http://stackoverflow.com/a/3077316)
          externalURL=`curl -Ls -o /dev/null -w '%{url_effective}' "$externalURL"`
          if [ $? != 0 ]; then
            echo "██ Failed to resolve URL ${BASH_REMATCH[1]}"
            exit 1
          fi
        else
          echo "██ No URL found for external"
          exit 1
        fi
        external=`echo -n "$external" | sed -re "s~$urlRegex~~" | sed -re "s~^[[:space:]]*/?([[:graph:]]*)[[:space:]]*$~\1~"`

        pathRegex="^([[:graph:]]+)$"
        if [[ $external =~ $pathRegex ]]; then
          # Path from the root of this repo where the contents of the external should be put
          externalPath="${BASH_REMATCH[1]}"
        else
          echo "██ Could not find external path"
          exit 1
        fi

        # the SHA1 hash of the URL, primarily used to get a string that can be used as directory name, is not too long and is different for different URLs
        externalURLSha=`echo -n "$externalURL" | sha1sum | cut -c-40`

        # the UUID of the repository, also used as directory name to group the URLs by repository
        tmpURL="$externalURL"
        # Shorten the URL if the full path is no longer present in the current revision
        while [ `echo "$tmpURL" | grep -o "/" | wc -l` -ge 2 ]; do
          if [ -z "$externalUUID" ]; then
            echo "██ Try to get UUID for URL $tmpURL"
            tmpUUID=`svn info "$tmpURL" --xml 2>/dev/null | grep -oE "<uuid>.{36}</uuid>" | cut -c7-42`
            if [ $? == 0 ] && [ "$tmpUUID" != "" ]; then
              externalUUID="$tmpUUID"
              tmpURL=""
            fi
          fi
          tmpURL=`echo "$tmpURL" | sed -re "s~^(.*)/.*~\1~"`
        done
        if [ -z "$externalUUID" ]; then
          echo "██ Could not get the UUID of the SVN external repository."
          exit 1
        fi

        # directory into which the external is cloned
        externalDir="$SVN_EXTERNALS_CLONE_DIR/$externalUUID/$externalURLSha"

        echo "██ Found external:"
        echo "█ Path: $externalPath"
        echo "█ URL: $externalURL"
        echo "█ URL-SHA1: $externalURLSha"
        echo "█ UUID: $externalUUID"
        echo "█ Revision: $externalRevision"
        echo "█ Local repo directory: $externalDir"

        if [ ! -f "$externalDir/.git/config" ]; then
          echo "██ Initialize the SVN external in $externalDir."
          git svn init "$externalURL" "$externalDir"
          if [ $? != 0 ]; then
            echo "██ Could not initialize SVN external $externalURL in directory $externalDir for commit $newRevision"
            exit 1
          fi
        fi

        # Fetch newest version of SVN external
        cd "$externalDir"
        echo "██ Update the local clone of the SVN external with the latest changes."
        git svn fetch -q
        if [ $? != 0 ]; then
          echo "██ Could not fetch new commits for SVN external $externalURL in directory $externalDir for commit $newRevision"
          exit 1
        fi

        if [ -z $externalRevision ]; then
          externalCommit=`git rev-list -n 1 --before="$commitDate" refs/remotes/git-svn`
          externalRevision=`git svn find-rev "$externalCommit"`
        else
          externalCommit=`git svn find-rev "r$externalRevision"`
        fi
        echo "██ Checking out to $FULL_CLONE_DIR/$externalPath"
        cd "$FULL_CLONE_DIR"
        git rm -r "$externalPath"
        mkdir -p "$externalPath"
        cd "$externalDir"
        GIT_WORK_TREE="$FULL_CLONE_DIR/$externalPath" git checkout -f "$externalCommit"
        if [ $? != 0 ]; then
          echo "██ Could not checkout revision $externalRevision of external."
          exit 1
        fi

        commitMessage+=$'\n'"$externalURL@$externalRevision → $externalPath"

        cd "$FULL_CLONE_DIR"
        git stage -f "./$externalPath"
      done <<< "$externals"
    fi

    echo "██ Finish off the merge commit"
    # Take the the current working copy (the new revision/commit plus its .gitignore(s) plus its externals) and amend the current head of the trunk branch to the very same state
    tmpCommit=`git rev-parse trunk`
    git branch -D trunk
    git checkout -qb trunk HEAD
    git reset --soft "$tmpCommit"
    GIT_COMMITTER_DATE="$commitDate" GIT_COMMITTER_NAME="$commitName" GIT_COMMITTER_EMAIL="$commitEmail" git commit --amend -m "$commitMessage" --allow-empty
    if [ $? != 0 ]; then
      echo "██ Could not amend the commit. Aborting now."
      exit 1
    fi

    if [ "$PUSH_TO_ORIGIN" = true ]; then
      git push origin trunk
    fi
  fi
done <<< "$newRevisions"

echo "██ List of SVN externals that are locally available as git-svn clones:"
find "$SVN_EXTERNALS_CLONE_DIR" -maxdepth 2 -type d -exec test -d "{}/.git" \; -and -exec printf "{}\n  " \; -and -exec git config -f "{}/.git/config" svn-remote.svn.url \;
