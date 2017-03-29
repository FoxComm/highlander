FROM python:3.6-alpine

RUN mkdir -p /data-import
WORKDIR /data-import
ADD . /data-import/

CMD python3 src/main.py 10.240.0.10 both 10
