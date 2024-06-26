FROM amd64/alpine:latest
USER root

ARG ANDROID_SDK_VERSION=8092744
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/cmdline-tools/tools/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator

RUN apk update && apk add --no-cache bash openssh wget unzip sed grep git file openjdk17-jre-headless && \
rm -rf /var/cache/apk/* && \
wget https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
unzip *tools*linux*.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/tools && \
rm *tools*linux*.zip && \
ssh-keygen -A && \
adduser -s /bin/bash jenkins -D && \
echo 'jenkins:Strongpa55word' | chpasswd && \
chown -R jenkins:jenkins /home/jenkins && \
chown -R jenkins:jenkins ${ANDROID_SDK_ROOT} && \
grep -q '^#*PasswordAuthentication' /etc/ssh/sshd_config && sed -i '/^#*PasswordAuthentication[[:space:]]yes/c\PasswordAuthentication no' /etc/ssh/sshd_config || echo 'PasswordAuthentication no' >> /etc/ssh/sshd_config && \
grep -q '^#*PermitRootLogin' /etc/ssh/sshd_config && sed -i '/^#*PermitRootLogin[[:space:]]/c\PermitRootLogin no #' /etc/ssh/sshd_config || echo 'PermitRootLogin no' >> /etc/ssh/sshd_config && \
apk del wget unzip sed --quiet

USER jenkins
WORKDIR /home/jenkins
RUN mkdir -p /home/jenkins/.ssh && mkdir -p /home/jenkins/build && \
echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDsxWrl38jhFwZFiJ8CXAR9yFERrHW8hZEMrfr8h37BHCweEpAnzq7Ue7rBLfe5FbxtmIXpm/nPwvCymZEV+BJCxgPQUpqbreJq/q4bus0REze74/i82q1wQ7AL0qLgRgqjnMfx/hx1JugKj0m6vEys9EWPHeHVI27DKojjgF2kO5pB4E94du8m54zwY/ujQ/tl9rr2euztUiQ4Cz5cOkl2In2oDC6EIO3b+NXv9yeEByHEDXKy/xUeO+JivG2sBhSKJSAvhQroKYK3RPq/AArfsheKe4zAvxkeQVjaCJ0DRSXqN9rz6x8QMOdhA/QkjSlKyg2VT4tRwGJveb1s2Sf akk0rd87@public-instance' >> /home/jenkins/.ssh/authorized_keys && \
chmod 600 /home/jenkins/.ssh/authorized_keys &&\
yes | sdkmanager --licenses && sdkmanager "platforms;android-31" "build-tools;30.0.2"

USER root
EXPOSE 22
CMD ["/usr/sbin/sshd","-D"]