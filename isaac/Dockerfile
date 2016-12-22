FROM ubuntu:16.04

RUN apt-get update && apt-get install -y  \
    postgresql-client-9.5 \
    libgoogle-glog0v5 \
    libbotan-1.10-1 \
    libpqxx-4.0 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir /isaac
ADD run_isaac.sh /isaac/
ADD build/src/isaac/isaac /isaac/

RUN mkdir /logs

WORKDIR /isaac

CMD /isaac/run_isaac.sh 2>&1 | tee /logs/isaac.log
