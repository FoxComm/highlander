#ifndef FLYFISH_QUERY_SERV_H
#define FLYFISH_QUERY_SERV_H

#include <sstream>
#include <limits>

//for http endpoint
//
#include <folly/Memory.h>
#include <folly/Portability.h>
#include <folly/json.h>
#include <folly/io/async/EventBaseManager.h>
#include <proxygen/httpserver/HTTPServer.h>
#include <proxygen/httpserver/RequestHandler.h>
#include <proxygen/httpserver/RequestHandlerFactory.h>
#include <proxygen/httpserver/ResponseBuilder.h>
#include <boost/lexical_cast.hpp>

namespace isaac
{
    namespace net
    {

        class query_request_handler : public proxygen::RequestHandler {
            public:
                explicit query_request_handler() : 
                    RequestHandler{} {}

                void onRequest(std::unique_ptr<proxygen::HTTPMessage> headers) noexcept override
                try
                {
                    if(headers->getPath() == "/validate") 
                        validate(*headers);
                    else
                    {
                        proxygen::ResponseBuilder(downstream_)
                            .status(404, "Not Found")
                            .sendWithEOM();
                    }

                }
                catch(std::exception& e)
                {
                    proxygen::ResponseBuilder(downstream_)
                        .status(500, e.what())
                        .sendWithEOM();
                }
                catch(...)
                {
                    proxygen::ResponseBuilder(downstream_)
                        .status(500, "Unknown Error")
                        .sendWithEOM();
                }

                void validate(proxygen::HTTPMessage& headers) 
                {
                    proxygen::ResponseBuilder(downstream_)
                        .status(200, "OK")
                        .body("testing")
                        .sendWithEOM();
                }

                void onBody(std::unique_ptr<folly::IOBuf> body) noexcept override 
                { 
                    if (_body) _body->prependChain(std::move(body));
                    else _body = std::move(body);
                }

                void onEOM() noexcept override {}
                void onUpgrade(proxygen::UpgradeProtocol proto) noexcept override {}

                void requestComplete() noexcept override 
                { 
                    delete this;
                }

                void onError(proxygen::ProxygenError err) noexcept override 
                { 
                    delete this;
                }

            private:
                std::unique_ptr<folly::IOBuf> _body;
        };

        class query_handler_factory : public proxygen::RequestHandlerFactory 
        {
            public:
                query_handler_factory() : 
                    proxygen::RequestHandlerFactory{} {}

            public:

                proxygen::RequestHandler* onRequest(
                        proxygen::RequestHandler* r, 
                        proxygen::HTTPMessage* m) noexcept override 
                {
                    return new query_request_handler;
                }

                void onServerStart(folly::EventBase* evb) noexcept { } 
                void onServerStop() noexcept { }
        };
    }
}
#endif
