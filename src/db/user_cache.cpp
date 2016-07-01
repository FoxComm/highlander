
#include "db/user_cache.hpp"
#include <iostream>

namespace isaac
{
    namespace db
    {
        template<class db_query_func>
        bool valid_user(
            std::size_t id, 
            int ratchet, 
            user_hash_map& u, 
            db_query_func db_valid_user)
        {
            auto i = u.find(id);
            if(i == u.end())
            {
                auto v = db_valid_user(id, ratchet);
                if(!v) return false;

                i =  u.insert(id, user_entry{ratchet}).first;
            }

            return i->second.ratchet == ratchet;
        }

        bool user_cache::valid_customer(std::size_t id, int ratchet, user& db)
        {
            return valid_user(id, ratchet, _c, 
                    [&](std::size_t id, int ratchet) 
                    { 
                        return db.valid_customer(id, ratchet);
                    });
        }

        bool user_cache::valid_admin(std::size_t id, int ratchet, user& db)
        {
            return valid_user(id, ratchet, _a, 
                    [&](std::size_t id, int ratchet) 
                    { 
                        return db.valid_admin(id, ratchet);
                    });
        }

        bool user_cache::invalidate_customer(std::size_t id)
        {
            _c.erase(id);
        }

        bool user_cache::invalidate_admin(std::size_t id)
        {
            _a.erase(id);
        }
    }
}

