FROM openjdk:8-alpine

RUN apk add openssl
RUN adduser -S bot

RUN mkdir /home/bot/bot
WORKDIR /home/bot/bot
COPY --chown=bot:root build/libs/*.jar ./bot.jar
COPY --chown=bot:root config.json.enc .
RUN mkdir data
RUN chown -R bot:root .
USER bot
