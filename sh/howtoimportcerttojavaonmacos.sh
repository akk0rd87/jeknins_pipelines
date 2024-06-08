openssl x509 -in <(openssl s_client -connect jenkins.popapp.org:8443 -prexit 2>/dev/null) -out /Users/amir/Downloads/jenkins_cert_june_2024.crt
sudo keytool -importcert -file /Users/amir/Downloads/jenkins_cert_june_2024.crt -alias jenkins_cert_june_2024 -keystore /Users/amir/Library/Java/JavaVirtualMachines/corretto-17.0.11/Contents/Home/lib/security/cacerts -storepass changeit
