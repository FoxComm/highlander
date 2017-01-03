#include <iostream>
#include <fstream>
#include <thread>

#include "util/dbc.hpp"
#include "service/handler.hpp"


//for handler
#include <proxygen/httpserver/HTTPServer.h>
#include <proxygen/httpserver/RequestHandler.h>
#include <proxygen/httpserver/RequestHandlerFactory.h>
#include <proxygen/httpserver/ResponseBuilder.h>

//for main
#include <boost/program_options.hpp>

//for db
#include <pqxx/pqxx>

using folly::EventBase;
using folly::EventBaseManager;
using folly::SocketAddress;

using Protocol = proxygen::HTTPServer::Protocol;
namespace po = boost::program_options;

po::options_description create_descriptions()
{
    po::options_description d{"Options"};
    const auto workers = std::thread::hardware_concurrency();

    d.add_options()
        ("help,h", "prints help")
        ("ip,b", po::value<std::string>()->default_value("0.0.0.0"), "ip to bind")
        ("http_port,p", po::value<std::uint16_t>()->default_value(9190), "http port")
        ("http2_port,P", po::value<std::uint16_t>()->default_value(9191), "http 2.0 port")
        ("db,d", po::value<std::string>()->default_value("host=127.0.0.1 dbname=ic user=ic"), "db connection string")
        ("workers,w", po::value<std::size_t>()->default_value(workers), "worker threads");

    return d;
}

po::variables_map parse_options(int argc, char* argv[], po::options_description& desc)
{
    po::variables_map v;
    po::store(po::parse_command_line(argc, argv, desc), v);
    po::notify(v);

    return v;
}

void test_db_connection(const std::string& conn)
{
    pqxx::connection c{conn};
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
    const auto db_conn = opt["db"].as<std::string>();
    const auto workers = opt["workers"].as<std::size_t>();

    //test db connection
    test_db_connection(db_conn);

    std::vector<proxygen::HTTPServer::IPConfig> IPs = {
        {SocketAddress(ip, http_port), Protocol::HTTP},
        {SocketAddress(ip, http2_port), Protocol::HTTP2},
    };

    bernardo::service::context ctx;

    proxygen::HTTPServerOptions options;
    options.threads = workers;
    options.listenBacklog = 2048;
    options.idleTimeout = std::chrono::milliseconds(60000);
    options.shutdownOn = {SIGINT, SIGTERM};
    options.enableContentCompression = true;
    options.handlerFactories = proxygen::RequestHandlerChain()
        .addThen<bernardo::service::query_handler_factory>(ctx)
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
