FROM rakudo-star

RUN apt update && apt install -y  \
    librdkafka++1 \
    libssl-dev

RUN zef update
RUN zef install PKafka HTTP::Client

RUN mkdir /sphex
ADD run_sphex.sh /sphex/
ADD counter.p6 /sphex/

WORKDIR /sphex


CMD ["/sphex/run_sphex.sh"]
