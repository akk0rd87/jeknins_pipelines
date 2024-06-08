## connect to container
docker exec -it --user root fervent_mclean bash

# build and run
docker build -t jenkinsagent --no-cache . && docker run -d --restart=always -p 8022:22 jenkinsagent