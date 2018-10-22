#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_6a62f901348d_key -iv $encrypted_6a62f901348d_iv -in codesigning.asc.enc -out build-support/signingkey.asc -d
    gpg --fast-import build-support/signingkey.asc
fi

