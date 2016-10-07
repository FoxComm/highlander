Isaac
=========================

Isaac is an auth service which validates JWT tokens as fast as possible. 
Maybe it will generate them one day too. 

Validation
=========================

The JWT token's signature and the user the token refers to is validated. If the
user does not exist, then the token is invalid. The JWT token will also have an
integer called a "ratchet" which will increment whenever a user requires previous
JWT tokens to be invalidated.

Building
==========================

Download Dependencies.

- [boost](boost.org) 
- [botan](https://botan.randombit.net/)
- [folly](https://github.com/facebook/folly)
- [proxygen](https://github.com/facebook/proxygen)
- [libpq++](https://www.postgresql.org/docs/7.2/static/libpqplusplus.html)

Go to isaac project directory and
run...

    mkdir build
    cd build
    cmake ..
    make -j

Run through docker
==========================

OS X: Docker Beta for Mac

(Boot2Docker is not applicable for this case.)

```bash
# copy public_key.pem to current directory
docker build .
docker tag $IMAGE isaac
sudo ifconfig lo0 alias 172.16.123.1
docker run -p 9190:9190 -p 9191:9191 --add-host=dbhost:172.16.123.1 isaac
```
