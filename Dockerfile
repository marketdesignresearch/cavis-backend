### Build container
FROM maven:3.5.2-jdk-8 AS build
ARG MECHLIB_BRANCH=master
ARG SATS_BRANCH=feat-integrate-mechlib

COPY --from=cplex:12.9 /opt/ibm/ILOG/CPLEX_Studio129/cplex/lib/cplex.jar cplex.jar

COPY pom.xml /usr/src/app/pom.xml
COPY src/ /usr/src/app/src/

# Clone necessary repos
RUN git clone https://github.com/marketdesignresearch/mechlib.git --single-branch --branch ${MECHLIB_BRANCH}
RUN git clone https://github.com/spectrumauctions/sats.git --single-branch --branch ${SATS_BRANCH}

RUN mvn install:install-file -Dfile=cplex.jar -DgroupId=cplex -DartifactId=cplex -Dversion=12.9 -Dpackaging=jar \
    && mvn clean install -f mechlib -DskipTests \
    && mvn clean install -f sats -DskipTests \
    && cd /usr/src/app \
    && mvn clean package -DskipTests

### Final container
FROM cplex:12.9
COPY --from=build /usr/src/app/target/cavis-backend-0.0.1-SNAPSHOT.jar /usr/app/run-server.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar", "-Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux", "/usr/app/run-server.jar"]