#include <iostream>
#include <fstream>
#include <thread>

#include "util/dbc.hpp"
#include "service/query.hpp"

//for checking sig
#include <botan/botan.h>
#include <botan/pubkey.h>
#include <botan/rsa.h>
#include <botan/x509_key.h>

//for main
#include <boost/asio/ip/host_name.hpp>
#include <boost/program_options.hpp>

using folly::EventBase;
using folly::EventBaseManager;
using folly::SocketAddress;

using Protocol = proxygen::HTTPServer::Protocol;
namespace po = boost::program_options;
namespace ip = boost::asio::ip;

namespace 
{
    const std::string RS256_EMSA = "EMSA3(SHA-256)";
}

po::options_description create_descriptions()
{
    po::options_description d{"Options"};
    const auto workers = std::thread::hardware_concurrency();

    d.add_options()
        ("help", "prints help")
        ("ip", po::value<std::string>()->default_value("0.0.0.0"), "ip to bind")
        ("http_port", po::value<std::uint16_t>()->default_value(7070), "http port")
        ("http2_port", po::value<std::uint16_t>()->default_value(7071), "http 2.0 port")
        ("public_key", po::value<std::string>()->default_value("public_key.pem"), "public key file")
        ("workers", po::value<std::size_t>()->default_value(workers), "worker threads");

    return d;
}

po::variables_map parse_options(int argc, char* argv[], po::options_description& desc)
{
    po::variables_map v;
    po::store(po::parse_command_line(argc, argv, desc), v);
    po::notify(v);

    return v;
}

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
    auto description = create_descriptions();
    auto opt = parse_options(argc, argv, description);

    if(opt.count("help"))
    {
        std::cout << description << std::endl;
        return 0;
    }

    const auto ip = opt["ip"].as<std::string>();
    const auto http_port = opt["http_port"].as<std::uint16_t>();
    const auto http2_port = opt["http2_port"].as<std::uint16_t>();
    const auto key_file = opt["public_key"].as<std::string>();
    const auto workers = opt["workers"].as<std::size_t>();

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
    options.threads = workers;
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

