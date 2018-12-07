## 在 src/main/proto目录下执行
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=../java --java_out=../java transfer/transfer.proto
protoc --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java --grpc-java_out=../java --java_out=../java discovery/discovery.proto

