#!/bin/bash

# docker build -t livy-image:v1 .
# docker run --rm -p 127.0.0.1:8998:8998 -ti livy-image:v1

curl -X POST -H "Content-Type: application/json" -d '{
  "file": "file:///tmp/project/simple-project-1.0.jar",
  "className": "com.gitlab.progxaker.sparkapplication.SimpleApp",
  "args": ["--master", "local[*]"]
}' http://127.0.0.1:8998/batches
echo
