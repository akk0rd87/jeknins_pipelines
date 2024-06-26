FROM amd64/alpine:latest
USER root

ENV ANDROID_SDK_ROOT='/opt/android-sdk'

RUN apk update && apk add --no-cache bash openssh sed grep git file perl openjdk17-jre-headless sdkmanager  && \
rm -rf /var/cache/apk/* && \
ssh-keygen -A && \
adduser -s /bin/bash jenkins -D && \
echo "jenkins:Strongpa55word" | chpasswd && \
chown -R jenkins:jenkins /home/jenkins && \
grep -q '^#*PasswordAuthentication' /etc/ssh/sshd_config && sed -i '/^#*PasswordAuthentication[[:space:]]yes/c\PasswordAuthentication no' /etc/ssh/sshd_config || echo 'PasswordAuthentication no' >> /etc/ssh/sshd_config && \
grep -q '^#*PermitRootLogin' /etc/ssh/sshd_config && sed -i '/^#*PermitRootLogin[[:space:]]/c\PermitRootLogin no #' /etc/ssh/sshd_config || echo 'PermitRootLogin no' >> /etc/ssh/sshd_config
#grep -q '^#*UsePAM' /etc/ssh/sshd_config && sed -i '/^#*UsePAM[[:space:]]yes.*/c\UsePAM no' /etc/ssh/sshd_config || echo 'UsePAM no' >> /etc/ssh/sshd_config && \
#grep -q '^#*KbdInteractiveAuthentication' /etc/ssh/sshd_config && sed -i '/^#*KbdInteractiveAuthentication[[:space:]]yes/c\KbdInteractiveAuthentication no' /etc/ssh/sshd_config || echo 'KbdInteractiveAuthentication no' >> /etc/ssh/sshd_config

USER jenkins
WORKDIR /home/jenkins
RUN mkdir -p /home/jenkins/.ssh && mkdir -p /home/jenkins/build && \
echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDsxWrl38jhFwZFiJ8CXAR9yFERrHW8hZEMrfr8h37BHCweEpAnzq7Ue7rBLfe5FbxtmIXpm/nPwvCymZEV+BJCxgPQUpqbreJq/q4bus0REze74/i82q1wQ7AL0qLgRgqjnMfx/hx1JugKj0m6vEys9EWPHeHVI27DKojjgF2kO5pB4E94du8m54zwY/ujQ/tl9rr2euztUiQ4Cz5cOkl2In2oDC6EIO3b+NXv9yeEByHEDXKy/xUeO+JivG2sBhSKJSAvhQroKYK3RPq/AArfsheKe4zAvxkeQVjaCJ0DRSXqN9rz6x8QMOdhA/QkjSlKyg2VT4tRwGJveb1s2Sf akk0rd87@public-instance' >> /home/jenkins/.ssh/authorized_keys && \
chmod 600 /home/jenkins/.ssh/authorized_keys

USER root
EXPOSE 22
CMD ["/usr/sbin/sshd","-D"]