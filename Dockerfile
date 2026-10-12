FROM ubuntu:latest
LABEL authors="katti"

ENTRYPOINT ["top", "-b"]