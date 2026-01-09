#!/bin/bash
protoc \
  --ts_proto_out=src/lib/generated \
  --ts_proto_opt=snakeToCamel=false \
  --ts_proto_opt=outputEncodeMethods=false \
  --ts_proto_opt=outputClientImpl=false \
  --ts_proto_opt=outputServices=false \
  --ts_proto_opt=forceLong=bigint \
  --ts_proto_opt=useDate=false \
  proto/*.proto
