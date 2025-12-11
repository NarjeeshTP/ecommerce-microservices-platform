FROM eclipse-temurin:17-jre-alpine
LABEL service="feature-flags"
LABEL maintainer="ecommerce-platform"
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/feature-flags-*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
EXPOSE 8091
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8091/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
