FROM public.ecr.aws/docker/library/openjdk:22-ea-17-slim-bookworm
COPY ./target/*.jar /home/app.jar
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED"
CMD ["java","-jar","/home/app.jar"]
