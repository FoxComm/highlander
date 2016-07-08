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

# make sure keys are exists
COPY public_key.pem /home/isaac

WORKDIR /home/isaac

RUN mkdir build
WORKDIR /home/isaac/build
RUN cmake .. && make -j
RUN cp ./src/isaac/isaac /bin/isaac

RUN echo "#!/bin/bash\n/bin/isaac --public_key=/home/isaac/public_key.pem --db=\"host=dbhost dbname=phoenix_development user=phoenix\"" > /bin/isaac.run
RUN chmod +x /bin/isaac.run

CMD ["/bin/isaac.run"]

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*