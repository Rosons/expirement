#!/bin/sh
set -e
# Bind mounts from host are often root-owned; app user must write chat files and vector store.
for d in /data/chat-files /data/vector-store; do
  mkdir -p "$d"
  chown -R app:app "$d"
  chmod -R u+rwx "$d"
done
exec su-exec app java -jar /app/app.jar
