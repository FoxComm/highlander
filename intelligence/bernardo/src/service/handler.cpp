#include "service/handler.hpp"
#include "cluster/cluster.hpp"

#include <folly/dynamic.h>
#include <folly/json.h>

#include <boost/lexical_cast.hpp>

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

            if(_msg->getPath() == "/sfind")  //returns simple response
                find(false);
            else if(_msg->getPath() == "/find")  //returns detailed response
                find(true);
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

        auto group_name = payload.find("group");
        if(group_name == payload.items().end())
            throw std::invalid_argument{"'group' is missing from payload"};

        if(!group_name->second.isString())
            throw std::invalid_argument{"'group' must be a string"};

        auto traits = payload.find("traits");
        if(traits == payload.items().end())
            throw std::invalid_argument{"'traits' object is missing from payload"};

        if(!traits->second.isObject())
            throw std::invalid_argument{"'traits' must be an object"};

        cluster::query q;
        q.scope = scope->second.getString();
        q.group_name = group_name->second.getString();
        q.traits = traits->second;

        return q;
    }

    void query_request_handler::find(const bool detailed_response)
    {
        if(!_body) throw std::invalid_argument{"payload expected"};

        auto body = _body->moveToFbString();
        auto payload = folly::parseJson(body);
        auto query = to_query(payload);

        auto group = cluster::group_for_query(_c.groups, query);
        if(group == nullptr)
        {
            std::stringstream s;
            s << "unable to find group " << query.group_name << " in scope " << query.scope;
            throw std::invalid_argument{s.str()};
        }

        auto compiled = cluster::compile_query(query, *group);

        auto result = group->find_cluster(compiled);
        if(result.cluster == group->clusters.end())
        {
            std::stringstream s;
            s << "unable to find best cluster for " << query.traits << " in group " << query.group_name;
            throw std::invalid_argument{s.str()};
        }

        folly::dynamic response;

        if(detailed_response)
        {
            response = folly::dynamic::object
                ("id", result.cluster->id)
                ("ref", result.cluster->reference)
                ("traits", result.cluster->traits)
                ("dist", result.distance);

            proxygen::ResponseBuilder{downstream_}
            .body(folly::toJson(response))
                .status(200, "OK")
                .sendWithEOM();
        }
        else
        {
            auto id = result.cluster->id;

            proxygen::ResponseBuilder{downstream_}
            .body(boost::lexical_cast<std::string>(id))
                .status(200, "OK")
                .sendWithEOM();
        }
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
