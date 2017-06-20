FROM python:3.6-slim

RUN mkdir -p /data-import
WORKDIR /data-import
ADD . /data-import/

CMD python3 src/main.py --adidas --host $API_HOST $CMD --max-products $MAX
