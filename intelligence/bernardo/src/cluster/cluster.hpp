#ifndef BERNARDO_CLUSTER_H
#define BERNARDO_CLUSTER_H

#include <vector>

#include <flann/flann.hpp>
#include <folly/dynamic.h>

namespace bernardo::cluster 
{

    using feature_vec = std::vector<float>;

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

    struct cluster
    {
        feature_vec features; 
    };

    struct definition
    {
        trait::definitions traits;
        distance_function distance_func;
    };

    using cluster_vec = std::vector<cluster>;

    using context = std::string;
    using resource_reference = std::string;
    using resource_id = std::string;

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
}

#endif
