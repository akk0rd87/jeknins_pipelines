docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 jenkinsagent
docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 alpineagent

docker build -t jenkinsagent --no-cache .
docker run -d --restart=always -p 8022:22 jenkinsagent

## arm64
docker run -d --platform linux/amd64 --restart=on-failure -p 8022:22 jenkinsagent

docker build . -t alpineagent
docker build --platform linux/amd64 -t alpineagent --no-cache .
docker build --platform linux/amd64 -t jenkinsagent --no-cache .
docker run -it --rm --platform linux/amd64 -p 8022:22  amd64/alpine:latest

## host
docker build -t alpinehost --no-cache .
docker run -d --restart=on-failure -p 8822:22 alpinehost


docker exec -it --user root fervent_mclean bash



docker build -t jenkinsagent --no-cache .



docker run -it --rm qemu/x86-64:ovmf