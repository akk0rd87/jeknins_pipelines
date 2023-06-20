docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 jenkinsagent
docker run -d --platform linux/amd64 --restart=on-failure -v /opt/android-sdk/:/opt/android-sdk/ -p 8022:22 alpineagent

docker build . -t alpineagent
docker build --platform linux/amd64 -t te-grafana-dashboards-toolchain --no-cache .
docker run -it --rm --platform linux/amd64 -p 8022:22  amd64/alpine:latest