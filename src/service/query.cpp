#include "service/query.hpp"
#include "util/dbc.hpp"

namespace isaac
{
    namespace net
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

            if(_msg->getPath() == "/customer") 
                validate(*_msg, false);
            else if(_msg->getPath() == "/admin") 
                validate(*_msg, true);
            else if(_msg->getPath() == "/ping") 
                ping();
            else
                is404();
        }
        catch(std::exception& e)
        {
            proxygen::ResponseBuilder{downstream_}
            .status(500, e.what())
                .sendWithEOM();
        }
        catch(...)
        {
            proxygen::ResponseBuilder{downstream_}
            .status(500, "Unknown Error")
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

        bool query_request_handler::check_signature(const util::jwt_parts& parts)
        {
            REQUIRE(_c.verifier);
            return util::check_jwt_signature(parts, *_c.verifier);
        }

        bool query_request_handler::verify_header(const folly::dynamic& header)
        {
            return header["alg"].asString() == "RS256";
        }

        bool query_request_handler::verify_user(const folly::dynamic& user, bool must_be_admin)
        {
            REQUIRE(_c.user_cache);

            auto id = user["id"].asInt();
            auto is_admin = user["admin"].asBool();
            auto ratchet = user.count("ratchet") ? user["ratchet"].asInt() : 0;

            if(id < 0 || ratchet < 0) return false;
            if(is_admin != must_be_admin) return false;

            return is_admin ? 
                _c.user_cache->valid_admin(id, ratchet, _db) : 
                _c.user_cache->valid_customer(id, ratchet, _db);
        }


        void query_request_handler::validate(proxygen::HTTPMessage& msg, bool must_be_admin) 
        {
            const auto& headers = msg.getHeaders();
            const auto& token = headers.rawGet(_c.token_header);
            if(token.empty()) 
            {
                token_missing();
                return;
            }

            util::jwt_parts parts;
            if(!util::get_jwt_parts(
                        parts, 
                        reinterpret_cast<const unsigned char*>(token.data()), 
                        token.size())) 
            {
                invalid_jwt();
                return;
            }

            if(!check_signature(parts))
            {
                signature_not_verified();
                return;
            }

            auto decoded_header = util::base64url_decode(parts.header);
            auto header = folly::parseJson(decoded_header);

            if(!verify_header(header))
            {
                invalid_header();
                return;
            }

            auto decoded_payload = util::base64url_decode(parts.payload);
            auto payload = folly::parseJson(decoded_payload);

            if(util::jwt_expired(payload))
            {
                token_expired();
                return;
            }

            if(!verify_user(payload, must_be_admin))
            {
                invalid_user();
                return;
            }

            proxygen::ResponseBuilder{downstream_}
                .status(200, "OK")
                .sendWithEOM();
        }

        void query_request_handler::token_missing()
        {
            std::stringstream e;
            e << "The JWT header `" << _c.token_header << "' not found"; 

            proxygen::ResponseBuilder{downstream_}
                .status(401, e.str())
                .sendWithEOM();
        }

        void query_request_handler::invalid_jwt()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(401, "Invalid JWT")
                .sendWithEOM();
        }

        void query_request_handler::invalid_header()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(401, "Only RS256 signatures are supported")
                .sendWithEOM();
        }

        void query_request_handler::invalid_user()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(401, "Invalid User")
                .sendWithEOM();
        }

        void query_request_handler::token_expired()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(401, "Token Expired")
                .sendWithEOM();
        }

        void query_request_handler::signature_not_verified()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(401, "Tampered token")
                .sendWithEOM();
        }
    }
}
