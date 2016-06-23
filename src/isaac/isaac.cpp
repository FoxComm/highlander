#include <iostream>
#include <fstream>

#include "util/dbc.hpp"
#include "service/query.hpp"
#include <unistd.h>

//for checking sig
#include <botan/botan.h>
#include <botan/pubkey.h>
#include <botan/rsa.h>
#include <botan/x509_key.h>

using folly::EventBase;
using folly::EventBaseManager;
using folly::SocketAddress;

using Protocol = proxygen::HTTPServer::Protocol;

const std::string RS256_EMSA = "EMSA3(SHA-256)";

bool load_key(isaac::net::context& c, const std::string& key_file)
{
    std::ifstream is{key_file.c_str()};
    if(!is) return false;

    Botan::DataSource_Stream fs{is};
    c.pub_key = Botan::X509::load_key(fs);
    std::unique_ptr<Botan::PK_Verifier> v{new Botan::PK_Verifier{*c.pub_key, RS256_EMSA}};
    c.verifier = std::move(v);
    return true;
}

int main(int argc, char** argv)
try
{
    const std::size_t WORKERS = 4 * sysconf(_SC_NPROCESSORS_ONLN);
    const std::string ip = "0.0.0.0";
    const std::uint16_t http_port = 7070;
    const std::uint16_t http2_port = 7071;

    const std::string key_file = "pk.pem";

    isaac::net::context ctx;
    if(!load_key(ctx, key_file))
    {
        std::cerr << "Unable to load key file: " << key_file << std::endl;
        return 1;
    }

    std::vector<proxygen::HTTPServer::IPConfig> IPs = {
        {SocketAddress(ip, http_port), Protocol::HTTP},
        {SocketAddress(ip, http2_port), Protocol::HTTP2},
    };

    proxygen::HTTPServerOptions options;
    options.threads = WORKERS;
    options.listenBacklog = 2048;
    options.idleTimeout = std::chrono::milliseconds(60000);
    options.shutdownOn = {SIGINT, SIGTERM};
    options.enableContentCompression = true;
    options.handlerFactories = proxygen::RequestHandlerChain()
        .addThen<isaac::net::query_handler_factory>(ctx)
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

