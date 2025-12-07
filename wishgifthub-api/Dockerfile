# Dockerfile pour le backend WishGiftHub uniquement
# Construit et déploie l'API Spring Boot

# ========================================
# Stage 1: Build OpenAPI
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS openapi-builder

WORKDIR /build

# Copier les fichiers pom.xml du parent et des modules
COPY ../pom.xml ./pom.xml
COPY ../wishgifthub-openapi/pom.xml ./wishgifthub-openapi/pom.xml
COPY pom.xml ./wishgifthub-api/pom.xml

# Télécharger les dépendances Maven
RUN mvn dependency:go-offline -B -pl wishgifthub-openapi || true

# Copier les sources OpenAPI
COPY ../wishgifthub-openapi/src ./wishgifthub-openapi/src

# Construire le module OpenAPI pour générer le fichier openapi.yaml
RUN mvn clean install -pl wishgifthub-openapi -am -DskipTests

# ========================================
# Stage 2: Build Backend Spring Boot
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /build

# Copier les fichiers pom.xml
COPY ../pom.xml ./pom.xml
COPY ../wishgifthub-openapi/pom.xml ./wishgifthub-openapi/pom.xml
COPY pom.xml ./wishgifthub-api/pom.xml

# Télécharger les dépendances Maven
RUN mvn dependency:go-offline -B || true

# Copier les sources OpenAPI depuis le stage précédent
COPY --from=openapi-builder /build/wishgifthub-openapi ./wishgifthub-openapi

# Copier les sources de l'API
COPY src ./wishgifthub-api/src

# Construire l'application Spring Boot
RUN mvn clean package -DskipTests -pl wishgifthub-api -am

# ========================================
# Stage 3: Runtime
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

