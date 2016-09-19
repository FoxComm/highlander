#ifndef ISAAC_UTIL_USER_CACHE_H
#define ISAAC_UTIL_USER_CACHE_H

#include <string>
#include <memory>
#include "db/user_verifier.hpp"
#include <folly/AtomicHashMap.h>

namespace isaac
{
    namespace db
    {
        using key = std::size_t;
        struct user_entry 
        {
            int ratchet;
        };

        using user_hash_map = folly::AtomicHashMap<key, user_entry>;
        
        class user_cache
        {
            public:
                user_cache(std::size_t user_size_est) : 
                    _u{user_size_est} {}

            public:
                bool valid_user(std::size_t id, int ratchet, user_verifier& verifier);

            public:
                bool invalidate_user(std::size_t id);

            private:
                user_hash_map _u;
        };
    }
}

#endif
