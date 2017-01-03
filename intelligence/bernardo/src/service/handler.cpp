#include "service/handler.hpp"
#include "cluster/cluster.hpp"

#include <folly/dynamic.h>
#include <folly/json.h>

#include <sstream>


namespace bernardo::service
{
    void query_request_handler::onRequest(std::unique_ptr<proxygen::HTTPMessage> msg) noexcept
    {
        _msg = std::move(msg);
    }

    void query_request_handler::onBody(std::unique_ptr<folly::IOBuf> body) noexcept
    { 
        if (_body) _body->prependChain(std::move(body));
        else _body = std::move(body);
    }

    void query_request_handler::onEOM() noexcept
        try
        {
            if(!_msg) return;

            if(_msg->getPath() == "/find") 
                find();
            else if(_msg->getPath() == "/ping") 
                ping();
            else
                is404();
        }
    catch(std::exception& e)
    {
        proxygen::ResponseBuilder{downstream_}
        .status(401, e.what())
            .sendWithEOM();
    }
    catch(...)
    {
        proxygen::ResponseBuilder{downstream_}
        .status(401, "Unknown Error")
            .sendWithEOM();
    }


    void query_request_handler::onUpgrade(proxygen::UpgradeProtocol proto) noexcept 
    {
    }

    void query_request_handler::requestComplete() noexcept
    { 
        delete this;
    }

    void query_request_handler::onError(proxygen::ProxygenError err) noexcept
    { 
        delete this;
    }

    cluster::query to_query(const folly::dynamic& payload)
    {
        auto scope = payload.find("scope");
        if(scope == payload.items().end())
            throw std::invalid_argument{"'scope' is missing from payload"};

        if(!scope->second.isString())
            throw std::invalid_argument{"'scope' must be a string"};

        auto type = payload.find("type");
        if(type == payload.items().end())
            throw std::invalid_argument{"'type' is missing from payload"};

        if(!type->second.isString())
            throw std::invalid_argument{"'type' must be a string"};

        auto traits = payload.find("traits");
        if(traits == payload.items().end())
            throw std::invalid_argument{"'traits' object is missing from payload"};

        if(!traits->second.isObject())
            throw std::invalid_argument{"'traits' must be an object"};

        cluster::query q;
        q.scope = scope->second.getString();
        q.type = type->second.getString();
        q.traits = traits->second;

        return q;
    }

    void query_request_handler::find()
    {
        if(!_body) throw std::invalid_argument{"payload expected"};

        auto body = _body->moveToFbString();
        auto payload = folly::parseJson(body);
        auto query = to_query(payload);

        cluster::all_groups all;
        cluster::group poo 
        {
            {
                {
                    {"foo", cluster::trait::kind::number, {}}, 
                    {"bar", cluster::trait::kind::enumeration, {"a", "b"}}
                },
                cluster::distance_function::euclidean
            },
            {}
        };
        poo.add_cluster(folly::dynamic::object("foo", 1)("bar", "b"));
        poo.add_cluster(folly::dynamic::object("foo", 2)("bar", "a"));
        poo.add_cluster(folly::dynamic::object("foo", 2)("bar", "b"));

        all.groups["1.2"]["poo"] = poo;

        std::cout << "PAYLOAD: " << payload << std::endl;
        std::cout << "TYPE: " << query.type << std::endl;
        std::cout << "TRAITS: " << query.traits << std::endl;

        auto group = cluster::group_for_query(all, query);
        if(group == nullptr)
        {
            std::stringstream s;
            s << "unable to find group " << query.type << " in scope " << query.scope;
            throw std::invalid_argument{s.str()};
        }

        auto compiled = cluster::compile_query(query, *group);

        std::cout << "COMPILED: ";
        for(auto v : compiled) std::cout << v << " ";
        std::cout << std::endl;

        auto result = find_cluster(compiled, *group);
        if(result.cluster == group->clusters.end())
        {
            std::stringstream s;
            s << "unable to find best cluster for " << query.traits << " in group " << query.type;
            throw std::invalid_argument{s.str()};
        }

        std::cout << "BEST MATCH SCORE: " << result.distance << std::endl;
        std::cout << "BEST MATCH: " << result.cluster->traits << " features: ";
        for(auto v : result.cluster->features) std::cout << v << " ";
        std::cout << std::endl;



        proxygen::ResponseBuilder{downstream_}
        .status(200, "OK")
            .sendWithEOM();
    }

    void query_request_handler::ping()
    {
        proxygen::ResponseBuilder{downstream_}
        .status(200, "pong")
            .sendWithEOM();
    }

    void query_request_handler::is404()
    {
        proxygen::ResponseBuilder{downstream_}
        .status(404, "Not Found")
            .sendWithEOM();
    }
}
