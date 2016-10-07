
#include "db/user_cache.hpp"
#include <iostream>

namespace isaac
{
    namespace db
    {
        template<class db_query_func>
        bool check_valid_user(
            std::size_t id, 
            int ratchet, 
            user_hash_map& u, 
            db_query_func valid_user)
        {
            auto i = u.find(id);
            if(i == u.end())
            {
                //check db, if not valid, return and don't cache
                if(!valid_user(id, ratchet)) return false;

                //otherwise user is valid so we cache here.
                i =  u.insert(id, user_entry{ratchet}).first;
            }

            return i->second.ratchet == ratchet;
        }

        bool user_cache::valid_user(std::size_t id, int ratchet, user_verifier& verifier)
        {
            return check_valid_user(id, ratchet, _u, 
                    [&](std::size_t id, int ratchet) 
                    { 
                        return verifier.valid_user(id, ratchet);
                    });
        }

        bool user_cache::invalidate_user(std::size_t id)
        {
            _u.erase(id);
        }
    }
}

