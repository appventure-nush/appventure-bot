FROM openjdk:8-alpine

RUN apk add lighttpd openssl
RUN apk add --no-cache openrc
RUN adduser -S bot
COPY --chown=bot:root index.html /var/www/localhost/
RUN rc-update add lighttpd default

USER bot
RUN mkdir /home/bot/bot
WORKDIR /home/bot/bot
COPY --chown=bot:root build/libs/*.jar ./bot.jar
COPY --chown=bot:root config.json.enc .
COPY --chown=bot:root lighttpd.conf .
