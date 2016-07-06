
#include "db/user.hpp"
#include "util/dbc.hpp"

#include <sstream>

namespace isaac
{
    namespace db
    {
        std::string query(const char* table, std::size_t id)
        {
            std::stringstream q;
            q << "select id from " << table << " where id=" << id << " limit 1";
            return q.str();
        }

        inline bool same_ratchet(const pqxx::result& result, const int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);
            if(result.empty()) return false;

            int db_ratchet = 0;
            result[0][0].to(db_ratchet);

            return ratchet == db_ratchet;
        }

        bool user::valid_customer(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            pqxx::read_transaction w{_c};
            auto r = w.exec(query("customers", id));
            return same_ratchet(r, ratchet);
        }

        bool user::valid_admin(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            pqxx::read_transaction w{_c};
            auto r = w.exec(query("store_admins", id));
            return same_ratchet(r, ratchet);
        }
    }
}
