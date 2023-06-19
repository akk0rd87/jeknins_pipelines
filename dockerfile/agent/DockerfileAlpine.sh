FROM amd64/alpine:latest
USER root

ENV ANDROID_SDK_ROOT='/opt/android-sdk'

RUN apk update && apk add --no-cache bash openssh sed grep git file  && \
rm -rf /var/cache/apk/* && \
ssh-keygen -A && \
adduser -s /bin/bash jenkins -D && \
echo "jenkins:Strongpa55word" | chpasswd && \
chown -R jenkins:jenkins /home/jenkins

USER jenkins
WORKDIR /home/jenkins
RUN mkdir -p /home/jenkins/.ssh && mkdir -p /home/jenkins/build && \
echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDsxWrl38jhFwZFiJ8CXAR9yFERrHW8hZEMrfr8h37BHCweEpAnzq7Ue7rBLfe5FbxtmIXpm/nPwvCymZEV+BJCxgPQUpqbreJq/q4bus0REze74/i82q1wQ7AL0qLgRgqjnMfx/hx1JugKj0m6vEys9EWPHeHVI27DKojjgF2kO5pB4E94du8m54zwY/ujQ/tl9rr2euztUiQ4Cz5cOkl2In2oDC6EIO3b+NXv9yeEByHEDXKy/xUeO+JivG2sBhSKJSAvhQroKYK3RPq/AArfsheKe4zAvxkeQVjaCJ0DRSXqN9rz6x8QMOdhA/QkjSlKyg2VT4tRwGJveb1s2Sf akk0rd87@public-instance' >> /home/jenkins/.ssh/authorized_keys && \
chmod 600 /home/jenkins/.ssh/authorized_keys

USER root
EXPOSE 22
CMD ["/usr/sbin/sshd","-D"]