#include "util/jwt.hpp"
#include "util/dbc.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/split.hpp>

//for base64
#include <boost/iterator/transform_iterator.hpp>
#include <boost/archive/iterators/binary_from_base64.hpp>
#include <boost/archive/iterators/transform_width.hpp>

namespace isaac
{
    namespace util
    {

        struct to_base64
        {
            typedef char result_type;
            result_type operator()(char c) const
            {
                if(c == '-') return '+';
                else if(c == '_') return '/';
                return c;
            }
        };

        bool get_jwt_parts(jwt_parts& parts, const char* data, size_t length)
        {
            parts_iterator it(data, data + length, 
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
            //decode base64
            using namespace boost::archive::iterators;
            using it = 
                transform_width<
                    binary_from_base64<
                        boost::transform_iterator<to_base64, const char*>>, 
                    8, 6>;

            return boost::algorithm::trim_right_copy_if(
                    std::string(it(p->begin()), it(p->end())), 
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

        bool jwt_expired(folly::dynamic& payload)
        {
            const auto curr_time = std::time(nullptr);
            const auto exp_time = payload["exp"].asInt();
            return curr_time >= exp_time;
        }
    }
}
