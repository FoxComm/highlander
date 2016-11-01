FROM narma/proxygen:1.0

RUN apt-get update
RUN apt-get install -y cmake libbotan1.10-dev \
    g++ \
    libboost-all-dev \
    libbotan1.10-dev \
    libpqxx-dev \
    libgoogle-glog-dev \
    make

RUN mkdir /isaac
ADD . /home/isaac
ADD run_isaac.sh /isaac

WORKDIR /home/isaac

RUN mkdir build
WORKDIR /home/isaac/build
RUN cmake .. && make -j
RUN cp ./src/isaac/isaac /isaac/isaac

# Clean up APT and build dir when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
RUN rm -rf /home/isaac

CMD ["/isaac/run_isaac.sh"]
