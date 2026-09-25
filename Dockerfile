FROM eclipse-temurin:25-jdk-noble as build

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=.gradle \
    ./gradlew :distTar

FROM eclipse-temurin:25-jdk-noble

WORKDIR /app

RUN --mount=type=bind,from=build,source=/app/build/distributions/IdeaLync.tar,target=IdeaLync.tar \
    tar xf IdeaLync.tar

CMD ["IdeaLync/bin/IdeaLync"]

