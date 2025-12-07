# Dockerfile pour le backend WishGiftHub uniquement
# Construit et déploie l'API Spring Boot

# ========================================
# Stage 1: Build OpenAPI puis api
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /build

# Copier le pom parent + modules
COPY pom.xml .
COPY wishgifthub-openapi/pom.xml ./wishgifthub-openapi/pom.xml
COPY wishgifthub-api/pom.xml ./wishgifthub-api/pom.xml

# Téléchargement des dépendances une seule fois
RUN mvn dependency:go-offline -B || true

# Copier les sources
COPY wishgifthub-openapi/src ./wishgifthub-openapi/src
COPY wishgifthub-api/src ./wishgifthub-api/src

# Build du module OpenAPI (génère le yaml)
RUN mvn install -pl wishgifthub-openapi -am -DskipTests

# Build du backend
RUN mvn package -pl wishgifthub-api -am -DskipTests

# ========================================
# Stage 2: Runtime
# ========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Installer wget pour le healthcheck
RUN apk add --no-cache wget

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Copier le JAR depuis le builder
COPY --from=backend-builder /build/wishgifthub-api/target/*.jar app.jar

# Changer le propriétaire
RUN chown spring:spring app.jar

# Passer à l'utilisateur non-root
USER spring:spring

# Exposer le port 8080
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Variables d'environnement pour la configuration
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Lancer l'application avec les options Java
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

