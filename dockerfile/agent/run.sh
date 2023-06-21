docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 jenkinsagent
docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 alpineagent


docker run -d --platform linux/amd64 --restart=on-failure -p 8022:22 jenkinsagent

docker build . -t alpineagent
docker build --platform linux/amd64 -t alpineagent --no-cache .
docker build --platform linux/amd64 -t jenkinsagent --no-cache .
docker run -it --rm --platform linux/amd64 -p 8022:22  amd64/alpine:latest