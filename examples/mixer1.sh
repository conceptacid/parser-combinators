#!/usr/bin/env bash
mkdir -p generated
mkdir -p generated/kotlin
mkdir -p generated/typescript
java -jar $PWD/../build/libs/idl-1.0-SNAPSHOT.jar kotlin -i mixer1 -o generated/kotlin
java -jar $PWD/../build/libs/idl-1.0-SNAPSHOT.jar typescript -i mixer1 -o generated/typescript