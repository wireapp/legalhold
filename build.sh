#!/usr/bin/env bash
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t $DOCKER_USERNAME/hold:staging .
docker push $DOCKER_USERNAME/hold
kubectl delete pod -l name=hold
kubectl get pods -l name=hold

