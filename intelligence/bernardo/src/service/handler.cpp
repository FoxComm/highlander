#include "service/handler.hpp"

namespace bernardo
{
    namespace service
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

            if(_msg->getPath() == "/ping") 
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
}
