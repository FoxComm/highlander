
#include "db/user.hpp"
#include "util/dbc.hpp"

#include <boost/optional.hpp>

#include <sstream>

namespace isaac
{
    namespace db
    {
        std::string query_str(const char* table, const std::size_t id)
        {
            std::stringstream q;
            q << "select id from " << table << " where id=" << id << " limit 1";
            return q.str();
        }


        boost::optional<int> get_db_ratchet(
                pqxx::connection& c, 
                const char* table, 
                const std::size_t id)
        {
            pqxx::read_transaction w{c};
            auto r = w.exec(query_str(table, id));
            if(r.empty()) return boost::none;

            int db_ratchet = 0;
            r[0][0].to(db_ratchet);

            return db_ratchet;
        }

        bool user::valid_customer(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            const auto db_ratchet = get_db_ratchet(_c, "customers", id);
            return db_ratchet && ratchet == db_ratchet.value();
        }

        bool user::valid_admin(std::size_t id, int ratchet)
        {
            REQUIRE_GREATER_EQUAL(ratchet, 0);

            const auto db_ratchet = get_db_ratchet(_c, "store_admins", id);
            return db_ratchet && ratchet == db_ratchet.value();
        }
    }
}
