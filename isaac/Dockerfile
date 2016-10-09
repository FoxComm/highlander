FROM narma/proxygen:1.0

RUN apt-get update
RUN apt-get install -y cmake libbotan1.10-dev \
    g++ \
    libboost-all-dev \
    libbotan1.10-dev \
    libpqxx-dev \
    libgoogle-glog-dev \
    make

ADD . /home/isaac

WORKDIR /home/isaac

RUN mkdir build
WORKDIR /home/isaac/build
RUN cmake .. && make -j
RUN cp ./src/isaac/isaac /bin/isaac

RUN echo "#!/bin/bash\n/bin/isaac --public_key=$PUBLIC_KEY --db=\"host=$DB_HOST dbname=$DB_NAME user=$DB_USER\"" > /bin/run_isaac.sh
RUN chmod +x /bin/run_isaac.sh

# Clean up APT and build dir when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
RUN rm -rf /home/isaac

CMD ["/bin/run_isaac.sh"]
