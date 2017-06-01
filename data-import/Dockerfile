FROM micktwomey/python3.4:latest

RUN mkdir -p /data-import
WORKDIR /data-import
ADD . /data-import/

CMD python3 src/main.py --adidas --host $API_HOST $CMD --max-products $MAX
