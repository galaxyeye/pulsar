#!/usr/bin/env bash

install_utils() {
  if ! command -v unzip &> /dev/null
  then
      sudo apt-get install unzip
  fi
}

install_chrome() {
    echo "Installing latest stable google-chrome"

    install_utils

    wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
    sudo dpkg -i google-chrome*.deb
    sudo apt-get install -f

    google-chrome -version
}

cd /tmp/ || exit

# find out chrome version
CHROME_VERSION="$(google-chrome -version | head -n1 | awk -F '[. ]' '{print $3}')"

if [[ "$CHROME_VERSION" == "" ]]; then
    install_chrome
fi

cd - || exit
