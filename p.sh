#!/bin/sh
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=target/generated-sources/protobuf/grpc-java --java_out=target/generated-sources/protobuf/java src/main/proto/transfer/transfer.proto
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=target/generated-sources/protobuf/grpc-java --java_out=target/generated-sources/protobuf/java src/main/proto/discovery/discovery.proto

