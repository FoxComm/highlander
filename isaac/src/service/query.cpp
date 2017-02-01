#include "service/query.hpp"
#include "util/dbc.hpp"

namespace isaac
{
    namespace net
    {
        namespace 
        {
            //this is arbitrary limit
            //to prevent abuse.
            const std::size_t MAX_JWT_SIZE = 1024*10;
        }

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

            //TODO: Add invalidation endpoints


            //Check endpoint just verifies if the user in the JWT is valid and 
            //that the JWT is signed
            if(_msg->getPath() == "/check") 
                check_token(*_msg);
            if(_msg->getPath() == "/check_role") 
                check_role(*_msg);
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

        bool query_request_handler::verify_user(const folly::dynamic& user)
        {
            REQUIRE(_c.user_cache);

            auto id = user["id"].asInt();
            auto ratchet = user["ratchet"].asInt();

            if(id < 0 || ratchet < 0) return false;

            return _c.user_cache->valid_user(id, ratchet, _db);
        }

        bool query_request_handler::user_has_role(const folly::dynamic& user, const std::string& role)
        {
            const auto roles = user["roles"];
            const auto pos =
                std::find_if(std::begin(roles), std::end(roles), [&role](const auto& v)
                {
                    return v.getString() == role;
                });

            return pos != std::end(roles);
        }

        token_data get_token(proxygen::HTTPMessage& msg, const std::string& key)
        {
            REQUIRE_GREATER(key.size(), 0);

            token_data r;

            //try to get token from the cookie
            auto cookie = msg.getCookie(key);
            if(!cookie.empty())
            {
                r.data = cookie.data();
                r.size = cookie.size();
            }
            //otherwise look at the headers
            else 
            {
                const auto& headers = msg.getHeaders();
                const auto& header = headers.getSingleOrEmpty(key);
                r.data = header.data();
                r.size = header.size();
            }

            ENSURE(r.size == 0 || r.data);
            return r;
        }

        void query_request_handler::check_role(proxygen::HTTPMessage& msg)
        {
            const auto token = get_token(msg, _c.token_header);

            if(msg.hasQueryParam("role"))
            {
                const auto role = msg.getQueryParam("role");
                validate_token(token, 
                        [&](const folly::dynamic& user) -> bool 
                        { 
                            return user_has_role(user, role) && verify_user(user); 
                        });
            }
            else
            {
                proxygen::ResponseBuilder{downstream_}
                .status(400, "role parameter missing")
                    .sendWithEOM();
            }
        }

        void query_request_handler::check_token(proxygen::HTTPMessage& msg)
        {
            const auto token = get_token(msg, _c.token_header);

            validate_token(token, [&](const folly::dynamic& user) -> bool { return verify_user(user); });
        }

        void query_request_handler::validate_token(const token_data& token, verify_user_func is_valid_user)
        {
            if(token.size == 0 || token.size >= MAX_JWT_SIZE) 
            {
                token_missing();
                return;
            }

            CHECK(token.data);
            CHECK_RANGE(token.size, 0, MAX_JWT_SIZE);

            util::jwt_parts parts;
            if(!util::get_jwt_parts(
                        parts, 
                        token.data, 
                        token.size)) 
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

            if(!is_valid_user(payload))
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
            e << "The JWT header/cookie `" << _c.token_header << "' not found"; 

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
