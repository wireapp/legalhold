#!/usr/bin/env bash
docker build -t $DOCKER_USERNAME/hold:latest .
docker push $DOCKER_USERNAME/hold
kubectl delete pod -l name=hold
kubectl get pods -l name=hold

