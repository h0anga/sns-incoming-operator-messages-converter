# sns incoming operator messages converter

TODO:
 - system end-to-end test
 - add proper readme
 
 This application *can* be run standalone, but is intended to be used as a Docker image from within Kubernetes.

Standalone - specifying the app-name, input and output topics:
sbt "run --app-name knitware-error-mxl-json-converter --input-topic KNITWARE_ERRORS_XML --output-topic KNITWARE_ERRORS"
 
 ##Local
 
 1) Ensure you have minikube and kubectl installed.
 2) Start minikube
 
 3) You will need to expose a port from the host where kafka is running, so that
 this application, running within a minikube pod, will be able to access it.
 Its not too difficult:
   - First, establish the IP address that virtualbox has assigned to 
 your minikube VM (its likely to be 192.168.99.1):
     ```
     ifconfig | grep vboxnet0 -A 2 | grep inet
     ```
   - Now, check the IP ("Cluster-IP) you've been assigned for your service:
       ```
       kubectl get services
       ```
   - You can access your host service (kafka!) using that IP and the port (9092).
   - Create the Environment variables the app needs within Kubernetes by running the following, substituting in your values for the server and port:
       ```
         kubectl create configmap kafka-broker-config --from-literal=KAFKA_BROKER_SERVER=10.103.3.240 --from-literal=KAFKA_BROKER_PORT=9092
       ```  
 3) Build the docker image using miniKube's docker:
     ```
     eval $(minikube docker-env)
     sbt docker:publishLocal
     ```
 4) Create and deploy a k8s pod with the application running within:
     ```
     kubectl apply -f sns-incoming-operator-messages-converter.yaml
     ```
 5) Check the pod status:
     ```
     kubectl get pod sns-incoming-operator-messages-converter
     kubectl describe pod sns-incoming-operator-messages-converter
     ```
 
 You could now try running the E2E tests!
 
 
 To stop all pods and remove a namespace:
 kubectl delete ns kafka