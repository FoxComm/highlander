FROM python:3.6-alpine

RUN mkdir -p /data-import
WORKDIR /data-import
ADD . /data-import/

CMD python3 src/main.py $API_HOST $CMD $MAX
