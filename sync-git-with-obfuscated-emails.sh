#!/bin/bash

# Us a clean directory every time. As the git history will get new hashes. Causing future pull/push to Azure to break.
rm -rf "temp"
mkdir "temp"
cd "temp"
git clone git@ssh.dev.azure.com:v3/ndwnu/NLS/nls-routing-map-matcher
cd nls-routing-map-matcher
git remote set-url origin git@github.com:ndwnu/nls-routing-map-matcher.git
git filter-branch -f --env-filter "
    GIT_AUTHOR_EMAIL='164055451+ndwnu@users.noreply.github.com'
    GIT_COMMITTER_EMAIL='164055451+ndwnu@users.noreply.github.com'
  " HEAD
git push
