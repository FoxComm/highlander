#ifndef FLYFISH_QUERY_SERV_H
#define FLYFISH_QUERY_SERV_H

#include "util/dbc.hpp"
#include "util/jwt.hpp"
#include "db/user_cache.hpp"

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
            std::string db_connection;
            std::string token_header;
            db::user_cache* user_cache;
            //TODO : remove me
            util::public_key* pub_key;
            util::sig_verifier_ptr verifier;
        };

        struct token_data
        {
            const char* data = nullptr;
            std::size_t size = 0;
        };

        using verify_user_func = std::function<bool(const folly::dynamic&)>;

        class query_request_handler : public proxygen::RequestHandler {
            public:
                explicit query_request_handler(context& c, db::user_verifier& db) : 
                    RequestHandler{}, _c{c}, _db{db} {}

                void onRequest(std::unique_ptr<proxygen::HTTPMessage> msg) noexcept override;
                void onBody(std::unique_ptr<folly::IOBuf> body) noexcept override;
                void onEOM() noexcept override;
                void onUpgrade(proxygen::UpgradeProtocol proto) noexcept override;
                void requestComplete() noexcept override;
                void onError(proxygen::ProxygenError err) noexcept override; 

            private:
                //services
                void check_token(proxygen::HTTPMessage& msg);
                void check_role(proxygen::HTTPMessage& msg);
                void validate_token(const token_data&, verify_user_func);
                void ping();
                void is404();

                //checks
                bool check_signature(const util::jwt_parts& parts);
                bool verify_header(const folly::dynamic&);
                bool verify_user(const folly::dynamic&);
                bool user_has_role(const folly::dynamic&, const std::string&);

                //errors
                void token_missing();
                void invalid_jwt();
                void invalid_header();
                void invalid_user();
                void token_expired();
                void signature_not_verified();

            private:

                context& _c;
                db::user_verifier& _db;
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
                    REQUIRE(_verifier);
                    return new query_request_handler{_c, *_verifier};
                }

                void onServerStart(folly::EventBase* evb) noexcept 
                { 
                    _db = std::make_unique<pqxx::connection>(_c.db_connection);
                    _verifier = std::make_unique<db::user_verifier>(*_db);

                    ENSURE(_db);
                    ENSURE(_verifier);
                } 

                void onServerStop() noexcept {} 

            private:
                context& _c;
                db::connection_ptr _db;
                db::verifier_ptr _verifier;
        };
    }
}
#endif
