#include "util/jwt.hpp"
#include "util/dbc.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/split.hpp>

//for base64
#include <boost/archive/iterators/binary_from_base64.hpp>
#include <boost/archive/iterators/base64_from_binary.hpp>
#include <boost/archive/iterators/transform_width.hpp>

namespace isaac
{
    namespace util
    {
        bool get_jwt_parts(jwt_parts& parts, folly::IOBuf& body)
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

            ENSURE(parts.payload->begin() > parts.header->end());
            ENSURE(parts.signature->begin() > parts.payload->end());
            ENSURE(parts.signature->end() - parts.header->begin() >= 2) // Should be at least ".."
            return true;
        }

        std::string base64url_decode(parts_iterator p)
        {
            //convert from base64url to base64
            //TODO get rid of this copy
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

        bool check_jwt_signature(const jwt_parts& parts, sig_verifier& verifier)
        {
            REQUIRE(parts.payload->end() > parts.header->begin());

            auto msg_size = parts.payload->end() - parts.header->begin();
            CHECK_GREATER_EQUAL(msg_size, 1); // should be at least '.'

            auto sig = base64url_decode(parts.signature);

            return verifier.verify_message(
                    reinterpret_cast<const Botan::byte*>(parts.header->begin()), msg_size,
                    reinterpret_cast<const Botan::byte*>(sig.data()), sig.size());
        }
    }
}
