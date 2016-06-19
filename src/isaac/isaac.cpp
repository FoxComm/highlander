#include <iostream>

#include "util/dbc.hpp"
#include "service/query.hpp"
#include <unistd.h>

using folly::EventBase;
using folly::EventBaseManager;
using folly::SocketAddress;

using Protocol = proxygen::HTTPServer::Protocol;

int main(int argc, char** argv)
try
{
    const std::size_t WORKERS = sysconf(_SC_NPROCESSORS_ONLN);
    const std::string ip = "0.0.0.0";
    const std::uint16_t http_port = 7070;
    const std::uint16_t http2_port = 7071;

    std::vector<proxygen::HTTPServer::IPConfig> IPs = {
        {SocketAddress(ip, http_port), Protocol::HTTP},
        {SocketAddress(ip, http2_port), Protocol::HTTP2},
    };

    proxygen::HTTPServerOptions options;
    options.threads = WORKERS;
    options.idleTimeout = std::chrono::milliseconds(60000);
    options.shutdownOn = {SIGINT, SIGTERM};
    options.enableContentCompression = true;
    options.handlerFactories = proxygen::RequestHandlerChain()
        .addThen<isaac::net::query_handler_factory>()
        .build();

    proxygen::HTTPServer query_server{std::move(options)};
    query_server.bind(IPs);

    //start server in a seperate thread.
    //We do this in case we want to add more servers in the future.
    std::thread query_thread
    {
        [&](){ query_server.start();}
    };

    query_thread.join();
    return 0;
}
catch(std::exception& e) 
{
    std::cerr << "Error! " << e.what() << std::endl;
    return 1;
}

