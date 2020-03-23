#!/bin/bash

cd "$(dirname `readlink -f "$0"`)"

cd ../../

docker image build -t javashebang_from_source -f ./scripts/docker/Dockerfile .
docker run -it javashebang_from_source
