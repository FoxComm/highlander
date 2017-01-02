#include "service/handler.hpp"
#include "cluster/cluster.hpp"

#include <folly/dynamic.h>
#include <folly/json.h>


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
        q.type = type->second.getString();
        q.traits = traits->second;

        return q;
    }

    void query_request_handler::find()
    {
        INVARIANT(_body);

        auto body = _body->moveToFbString();
        auto payload = folly::parseJson(body);
        auto query = to_query(payload);

        std::cout << "PAYLOAD: " << payload << std::endl;
        std::cout << "TYPE: " << query.type << std::endl;
        std::cout << "TRAITS: " << query.traits << std::endl;

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
