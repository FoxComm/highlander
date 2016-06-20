#include "service/query.hpp"
#undef CHECK
#include "util/dbc.hpp"

#include <sstream>
#include <limits>
#include <memory>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/split.hpp>

//for base64
#include <boost/archive/iterators/binary_from_base64.hpp>
#include <boost/archive/iterators/base64_from_binary.hpp>
#include <boost/archive/iterators/transform_width.hpp>

namespace isaac
{
    namespace net
    {
        namespace 
        {
            bool get_parts(part_ranges& parts, folly::IOBuf& body)
            {
                parts_iterator it(body.data(), body.data() + body.length(), 
                    boost::algorithm::first_finder(".", boost::algorithm::is_equal()));

                parts.header = it;
                it++;
                if(it == parts_iterator{}) return false;

                parts.payload = it;
                it++;
                if(it == parts_iterator{}) return false;

                parts.signature = it;
                it++;
                if(it != parts_iterator{}) return false;

                return true;
            }

            std::string base64url_decode(parts_iterator p)
            {
                //convert from base64url to base64
                std::string s{p->begin(), p->end()};
                std::transform(std::begin(s), std::end(s), std::begin(s), 
                        [](auto c)
                        {
                            if(c == '-') return '+';
                            else if(c == '_') return '/';
                            return c;
                        });

                //decode base64
                using namespace boost::archive::iterators;
                using it = transform_width<binary_from_base64<std::string::const_iterator>, 8, 6>;
                return boost::algorithm::trim_right_copy_if(
                        std::string(it(std::begin(s)), it(std::end(s))), 
                        [](char c) { return c == '\0'; }
                        );
            }

        }

        void query_request_handler::onRequest(std::unique_ptr<proxygen::HTTPMessage> headers) noexcept
        {
            _headers = std::move(headers);
        }

        void query_request_handler::onBody(std::unique_ptr<folly::IOBuf> body) noexcept
        { 
            if (_body) _body->prependChain(std::move(body));
            else _body = std::move(body);
        }

        void query_request_handler::onEOM() noexcept
        try
        {
            if(!(_headers && _body)) return;

            if(_headers->getPath() == "/validate") 
                validate(*_headers, *_body);
            else
            {
                proxygen::ResponseBuilder{downstream_}
                .status(404, "Not Found")
                    .sendWithEOM();
            }
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

        bool query_request_handler::check_signature(const part_ranges& parts)
        {
            REQUIRE(_c.verifier);

            auto msg_size = parts.payload->end() - parts.header->begin();
            auto sig = base64url_decode(parts.signature);

            return _c.verifier->verify_message(
                    reinterpret_cast<const Botan::byte*>(parts.header->begin()), msg_size,
                    reinterpret_cast<const Botan::byte*>(sig.data()), sig.size());
        }

        bool query_request_handler::verify_header(const folly::dynamic& header)
        {
            return header["alg"].asString() == "RS256";
        }

        bool query_request_handler::verify_user(const folly::dynamic& user)
        {
            auto id = user["id"].asInt();
            auto is_admin = user["admin"].asBool();
            auto ratchet = user.count("ratchet") ? user["ratchet"].asInt() : 0;

            std::cerr << "id: " << id << std::endl;
            std::cerr << "is_admin: " << is_admin << std::endl;
            std::cerr << "ratchet: " << ratchet << std::endl;

            //TODO
            //Hit cache, otherwise hit DB
            return true;
        }


        void query_request_handler::validate(proxygen::HTTPMessage& headers, folly::IOBuf& body) 
        {
            part_ranges parts;
            if(!get_parts(parts, body)) 
            {
                invalid_jwt();
                return;
            }

            if(!check_signature(parts))
            {
                signature_not_verified();
                return;
            }

            auto decoded_header = base64url_decode(parts.header);
            auto header = folly::parseJson(decoded_header);

            if(!verify_header(header))
            {
                invalid_header();
                return;
            }

            auto decoded_payload = base64url_decode(parts.payload);
            auto payload = folly::parseJson(decoded_payload);

            if(!verify_user(payload))
            {
                invalid_user();
                return;
            }

            proxygen::ResponseBuilder{downstream_}
                .status(200, "OK")
                .sendWithEOM();
        }

        void query_request_handler::invalid_jwt()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Invalid JWT")
                .sendWithEOM();
        }

        void query_request_handler::invalid_header()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Only RS256 signatures are supported")
                .sendWithEOM();
        }

        void query_request_handler::invalid_user()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Invalid User")
                .sendWithEOM();
        }

        void query_request_handler::signature_not_verified()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Signature is not valid")
                .sendWithEOM();
        }
    }
}
