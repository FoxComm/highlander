#ifndef BERNARDO_QUERY_SERV_H
#define BERNARDO_QUERY_SERV_H

#include "util/dbc.hpp"
#include "cluster/cluster.hpp"

#include <proxygen/httpserver/HTTPServer.h>
#include <proxygen/httpserver/RequestHandler.h>
#include <proxygen/httpserver/RequestHandlerFactory.h>
#include <proxygen/httpserver/ResponseBuilder.h>

namespace bernardo
{
    namespace service
    {
        struct context
        {
            const cluster::all_groups& groups;
        };

        class query_request_handler : public proxygen::RequestHandler {
            public:
                explicit query_request_handler(context& c) : 
                    RequestHandler{}, _c{c} {}

                void onRequest(std::unique_ptr<proxygen::HTTPMessage> msg) noexcept override;
                void onBody(std::unique_ptr<folly::IOBuf> body) noexcept override;
                void onEOM() noexcept override;
                void onUpgrade(proxygen::UpgradeProtocol proto) noexcept override;
                void requestComplete() noexcept override;
                void onError(proxygen::ProxygenError err) noexcept override; 

            private:
                void find(const bool detailed_response);
                void ping();
                void is404();

            private:

                context& _c;
                std::unique_ptr<folly::IOBuf> _body;
                std::unique_ptr<proxygen::HTTPMessage> _msg;
        };

        class query_handler_factory : public proxygen::RequestHandlerFactory 
        {
            public:
                query_handler_factory(context& c) : 
                    proxygen::RequestHandlerFactory{}, _c{c} {}

            public:
                proxygen::RequestHandler* onRequest(
                        proxygen::RequestHandler* r, 
                        proxygen::HTTPMessage* m) noexcept override 
                {
                    return new query_request_handler{_c};
                }

                void onServerStart(folly::EventBase* evb) noexcept {} 
                void onServerStop() noexcept {} 

            private:
                context& _c;
        };
    }
}
#endif
