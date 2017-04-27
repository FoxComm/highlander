FROM ubuntu:16.10

RUN apt update && apt install -y \
    postgresql-client-9.5 \
    libgoogle-glog0v5 \
    libssl1.0.0 \ 
    libdouble-conversion1v5 \
    libpqxx-4.0v5

RUN mkdir /bernardo
ADD run_bernardo.sh /bernardo/
ADD build/src/bernardo/bernardo /bernardo/

WORKDIR /bernardo

CMD ["/bernardo/run_bernardo.sh"]
