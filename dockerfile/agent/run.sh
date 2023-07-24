# build
docker build --platform linux/amd64 -t jenkinsagent --no-cache .
# run
docker run -d --restart=always --platform linux/amd64 -p 8022:22 jenkinsagent


## host
docker build -t alpinehost --no-cache .
docker run -d --restart=on-failure -p 8822:22 alpinehost

## connect to container
docker exec -it --user root fervent_mclean bash

docker run --privileged --rm tonistiigi/binfmt:master --install amd64

#
docker build -t jenkinsagent --no-cache . && docker run -d --restart=always -p 8022:22 jenkinsagent