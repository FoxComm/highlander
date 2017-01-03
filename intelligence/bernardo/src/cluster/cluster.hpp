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

    struct group
    {
        definition def;
        cluster_vec clusters;
    };

    using group_map = std::unordered_map<std::string, group>;

    struct all_groups
    {
        group_map groups;
    };

    group_map::const_iterator group_for_query(const all_groups&, const query&);
    feature_vec compile_query(const query&, const group&);
}

#endif
