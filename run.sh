#!/usr/bin/env bash

rm modified-aligned.apk modified.apk keystore.jks

# set -e

keytool -genkey -alias production -keyalg RSA -keystore keystore.jks -dname "CN=Mark Smith, OU=JavaSoft, O=Sun, L=Cupertino, S=California, C=US" -storepass password -keypass password

java ZipModifier.java --in=original.apk --out=modified.apk --add=config.properties:config.properties --rm=META-INF/CERT.SF --rm=META-INF/CERT.RSA

/app/android/zipalign -f 4 modified.apk modified-aligned.apk

java -jar /app/android/apksigner.jar sign -in modified-aligned.apk --out modified-aligned-signed..apk --ks keystore.jks --ks-key-alias production --ks-pass pass:password --key-pass pass:password --min-sdk-version=27 --debuggable-apk-permitted
