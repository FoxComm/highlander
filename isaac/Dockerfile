FROM ubuntu:16.04

RUN apt update && apt install -y  \
    postgresql-client-9.5 \
    libgoogle-glog0v5 \ 
    libbotan-1.10-1 \
    libpqxx-4.0

RUN mkdir /isaac
ADD run_isaac.sh /isaac/
ADD build/src/isaac/isaac /isaac/

WORKDIR /isaac


CMD ["/isaac/run_isaac.sh"]
