#ifndef FLYFISH_QUERY_SERV_H
#define FLYFISH_QUERY_SERV_H

#include "util/jwt.hpp"

#include <proxygen/httpserver/HTTPServer.h>
#include <proxygen/httpserver/RequestHandler.h>
#include <proxygen/httpserver/RequestHandlerFactory.h>
#include <proxygen/httpserver/ResponseBuilder.h>

namespace isaac
{
    namespace net
    {
        struct context
        {
            util::public_key* pub_key;
            util::sig_verifier_ptr verifier;
        };

        class query_request_handler : public proxygen::RequestHandler {
            public:
                explicit query_request_handler(context& c) : 
                    RequestHandler{}, _c{c} {}

                void onRequest(std::unique_ptr<proxygen::HTTPMessage> headers) noexcept override;
                void onBody(std::unique_ptr<folly::IOBuf> body) noexcept override;
                void onEOM() noexcept override;
                void onUpgrade(proxygen::UpgradeProtocol proto) noexcept override;
                void requestComplete() noexcept override;
                void onError(proxygen::ProxygenError err) noexcept override; 

            private:

                bool check_signature(const util::jwt_parts& parts);
                bool verify_header(const folly::dynamic&);
                bool verify_user(const folly::dynamic&);

                void validate(proxygen::HTTPMessage& headers, folly::IOBuf& body);

                //errors
                void invalid_jwt();
                void invalid_header();
                void invalid_user();
                void signature_not_verified();

            private:
                context& _c;
                std::unique_ptr<folly::IOBuf> _body;
                std::unique_ptr<proxygen::HTTPMessage> _headers;
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

                void onServerStart(folly::EventBase* evb) noexcept { } 
                void onServerStop() noexcept { }

            private:
                context& _c;
        };
    }
}
#endif
