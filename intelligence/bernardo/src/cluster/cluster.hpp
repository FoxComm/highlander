#ifndef BERNARDO_CLUSTER_H
#define BERNARDO_CLUSTER_H

#include <vector>
#include <memory>

#include <flann/flann.hpp>
#include <folly/dynamic.h>

#include <pqxx/pqxx>

namespace bernardo::cluster 
{

    using feature_vec = std::vector<double>;

    struct query
    {
        std::string scope;
        std::string group_name;
        folly::dynamic traits;
    };

    namespace trait
    {
        enum kind { number, enumeration};
        using enum_values = std::vector<std::string>;
        struct definition
        {
            std::string name;
            kind type;
            enum_values values;
        };

        using definitions =  std::vector<definition>;
    }

    enum distance_function { euclidean, hamming};

    struct definition
    {
        trait::definitions traits;
        distance_function distance_func;
    };

    struct cluster
    {
        std::string reference;
        folly::dynamic traits;
        feature_vec features; 
    };

    using cluster_vec = std::vector<cluster>;
    using indices_vec = std::vector<int>;
    using feature_mat = flann::Matrix<double>;
    using indices_mat = flann::Matrix<int>;
    using dist_mat = flann::Matrix<double>;

    //TODO: We will need several index types based on the distance function.
    //For now just do L2 which is squared euclidean
    using index = flann::Index<flann::L2<double>>;
    using index_ptr = std::unique_ptr<index>;

    struct find_result
    {
        cluster_vec::const_iterator cluster;
        double distance;
    };

    class group
    {
        public:
            definition def;
            cluster_vec clusters;

        public:
            group();
            group(const definition&);
            group& operator=(const group& o);

            void add_cluster(const std::string& reference, folly::dynamic attributes);
            find_result find_cluster(const feature_vec&) const;

            void build_index();

        private:
            feature_mat _features;
            index_ptr _index;
    };

    using group_map = std::unordered_map<std::string, group>;
    using scope_map = std::unordered_map<std::string, group_map>;

    struct all_groups
    {
        scope_map groups;
    };

    const group* group_for_query(const all_groups&, const query&);
    feature_vec compile_query(const query&, const group&);

    void load_groups_from_db(pqxx::connection&, all_groups&);
}

#endif
