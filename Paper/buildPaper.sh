#!/bin/sh
# Build PaperMC (paperclip.jar)

# install git
apt-get update
apt-get install -y git patch

# configure git (without paper will not build)
git config --global user.email "docker@marcermarc.de"
git config --global user.name "marcermarc"

# select version-branch from paper repo
if [ "$VERSION" = "latest" ]; then \
    export VERSION="master" \
; elif [ -n "$(git ls-remote -h https://github.com/PaperMC/Paper.git "ver/$VERSION")" ] ; then \
    export VERSION="ver/$VERSION" \
; elif [ -n "$(git ls-remote -h https://github.com/PaperMC/Paper.git "ver/$(echo $VERSION | cut -c 1-4)")" ] ; then \
    export VERSION="ver/$(echo $VERSION | cut -c 1-4)" \
; else \
    export VERSION="master" \
; fi

# clone paper
git clone -b ${VERSION} --depth 1 https://github.com/PaperMC/Paper.git /tmp/paper

# build paper
cd /tmp/paper
bash ./paper j