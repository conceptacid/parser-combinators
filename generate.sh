#!/usr/bin/env bash
mkdir examples/generated
java -jar $PWD/build/libs/idl-1.0-SNAPSHOT.jar typescript -i src/test -o examples/generated