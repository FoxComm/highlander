#ifndef BERNARDO_CLUSTER_H
#define BERNARDO_CLUSTER_H

#include <vector>

#include <flann/flann.hpp>
#include <folly/dynamic.h>

namespace bernardo::cluster 
{

    using feature_vec = std::vector<double>;

    struct query
    {
        std::string scope;
        std::string type;
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

    struct context 
    {
        folly::dynamic attributes;
    }; 

    using context_map = std::unordered_map<std::string, context>; 

    struct cluster
    {
        context_map contexts;
        feature_vec features; 
    };

    using cluster_vec = std::vector<cluster>;

    struct group
    {
        definition def;
        cluster_vec clusters;
    };

    using group_map = std::unordered_map<std::string, group>;
    using scope_map = std::unordered_map<std::string, group_map>;

    struct all_groups
    {
        scope_map groups;
    };

    const group* group_for_query(const all_groups&, const query&);
    feature_vec compile_query(const query&, const group&);

    struct find_result
    {
        cluster_vec::const_iterator cluster;
        double distance;
    };

    find_result find_cluster(const feature_vec&, const group&);
}

#endif
