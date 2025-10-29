# face-login-backend
# create database

docker-compose -f docker-compose.yml up -d

Start up

./mvnw spring-boot:run -Pdebug

curl -F "url=https://untransient-unaccountable-wendie.ngrok-free.dev/api/telegram/webhook" \
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook
<!-- example -->

token 8461269511:AAFWHLEm_cAGUY7tR9F_rtdsCFEIsmnx-ww
curl -F "url=https://untransient-unaccountable-wendie.ngrok-free.dev/api/telegram/webhook" \
https://api.telegram.org/bot<8461269511:AAFWHLEm_cAGUY7tR9F_rtdsCFEIsmnx-ww>/setWebhook


install 
brew install ngrok/ngrok/ngrok
-facelogin
-Password 
Mater@168 
Mail Rupp



# config debug add this to pom.xml
	<!-- debug -->
	<profiles>
    <profile>
        <id>debug</id>
        <properties>
            <spring-boot.run.jvmArguments>
                -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
            </spring-boot.run.jvmArguments>
        </properties>
    </profile>
</profiles>
	<!-- debug -->
