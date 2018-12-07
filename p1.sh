#!/bin/sh
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=src/main/java --java_out=src/main/java src/main/proto/transfer/transfer.proto
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=src/main/java --java_out=src/main/java/ src/main/proto/discovery/discovery.proto

