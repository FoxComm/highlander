#cluster

This library has the code to manage cluster groups and query for the best cluster.
This is where the meat and potatoes are.

##group

A group is composed of two parts
  1. definition
  2. clusters

##cluster definition
The definition is used to understand how to convert traits to a feature vector
so that you can compare a query against a cluster. It defines which traits all
clusters share.

    1. distance function.
    2. traits

A trait can either be a number or an enumeration. This allows us to map a trait to a number

##cluster

A cluster is a group of like data and is composed of two parts.

    1. traits
    2. feature vector

The traits are compiled into a feature vector for comparison

##query

A query is composed of traits and a group name. We can compile the query into
a feature vector using a group's cluster definition. Then take the feature vector
and find the best matching cluster in the group.

#FLANN

[Lib FLANN](http://www.cs.ubc.ca/research/flann/) is used to take a group's cluster
and index them so we can find the best cluster quickly.

