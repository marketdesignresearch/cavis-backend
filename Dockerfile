FROM maven:3.5.2-jdk-8 AS build
WORKDIR /usr/src/app
COPY pom.xml pom.xml
RUN mvn verify clean --fail-never
COPY --from=cplex:12.9 /opt/ibm/ILOG/CPLEX_Studio129/cplex/lib/cplex.jar cplex.jar
COPY --from=cplex:12.9 /ibm/ILOG/CPLEX_Studio129/cplex/lib/cplex.jar cplex.jar
COPY src/ src/
RUN mvn install:install-file -Dfile=cplex.jar -DgroupId=cplex -DartifactId=cplex -Dversion=12.9 -Dpackaging=jar \
        && rm cplex.jar \
        && mvn package -DskipTests

FROM cplex:12.9
COPY --from=build /usr/src/app/target/cavis-backend-0.0.1-SNAPSHOT.jar /usr/app/run-server.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar", "-Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux", "/usr/app/run-server.jar"]