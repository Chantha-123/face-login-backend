# face-login-backend

Backend service for face login using Spring Boot.  
This guide covers setup, running, debug, and Telegram webhook configuration with ngrok.

---

## Prerequisites

- Java 21
- Maven
- Docker & Docker Compose or install mysql
- ngrok (macOS: `brew install ngrok/ngrok/ngrok`)
- Telegram Bot token

---
## Docker Setup
## 1.Install Docker 
```
    https://docs.docker.com/desktop/setup/install/windows-install/
```
## 2.Start Database 

```bash
docker-compose -f docker-compose.yml up -d
```
---
## 3.Create telegram bot 

## Go to this user name of telegram
```
@BotFather

```
## or this link
```
https://t.me/BotFather

```
## give new botname for system
    /newbot
## curren bot is 
```
FaceLoginDIPBot
```
## save your token for bot given

## Done! Congratulations on your new bot. You will find it at t.me/FaceLoginDIPBot. You can now add a description, about section and profile picture for your bot, see /help for a list of commands. By the way, when you've finished creating your cool bot, ping our Bot Support if you want a better username for it. Just make sure the bot is fully operational before you do this.

## Use this token to access the HTTP API:
## 8461269511:AAFWHLEm_cAGUY7tR9F_rtdsCFEIsmnx-ww
## Keep your token secure and store it safely, it can be used by anyone to control your bot.

## For a description of the Bot API, see this page: https://core.telegram.org/bots/api


## After create telegram bot 

In the project file go to file application.yml

find telegram bot replace with new token and chat-id for admin
##
telegram:
  bot:
    token: "8461269511:AAFWHLEm_cAGUY7tR9F_rtdsCFEIsmnx-ww" # Replace with your Telegram bot token
  admin:
    chat-id: "789748955" # Replace with your Telegram admin chat ID  optional when use webhook


## 5. Set Telegram Webhook



## Notes / Credentials
## create account  to get service telegram

https://dashboard.ngrok.com/get-started/setup/macos

- ngrok account: `facelogin` (Free plan)  
- Password: `Mater@168`  
- Mail: `Rupp`  

Replace `<YOUR_BOT_TOKEN>` with your bot token:

```bash
curl -F "url=https://<ngrok_url>/api/telegram/webhook" \
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook
```

**Example:** 

```bash
curl -F "url=https://untransient-unaccountable-wendie.ngrok-free.dev/api/telegram/webhook" \
https://api.telegram.org/bot8461269511:AAFWHLEm_cAGUY7tR9F_rtdsCFEIsmnx-ww/setWebhook
```

---


## 6. Start Spring Boot with Debug strat only spring boot

Run with debug profile (port 5005):

```bash
./mvnw spring-boot:run -Pdebug
```

## 7. Start ngrok


```bash
ngrok http 8081
```

Check the forwarding URL:

```
https://<random>.ngrok-free.dev -> http://localhost:8081
```

---

## 8. One-line Command: Start Spring Boot + ngrok

For convenience, run Spring Boot and ngrok together:

```bash
./mvnw spring-boot:run -Pdebug & ngrok http 8081

```
checke port 
lsof -i :5005
Kill port
kill -9 12345 

<!-- Build PackageWar for tomcat -->
./mvnw clean package -DskipTests





