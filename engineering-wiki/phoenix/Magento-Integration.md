## Overview

We believe that the majority of our targeted customer base is using Magento. Providing features to fold their data into FoxComm is a major selling point. 

There are probably two ways to integrate with Magento; (1) via a data migration and (2) in some kind of cooperative integration, Magento still functions but customers lean on FoxComm services around it.

This is a big undertaking. This analysis can be a starting point for breaking it down into pieces so we can work out a way to approach it.


### Starting Magento locally with docker.

Follow these [instructions](https://registry.hub.docker.com/u/paimpozhil/magento-docker/). Everything is self explanatory except for the step where you wish to connect to the running magento instance if you are using OS X. In this circumstance you need to connect to the Docker Host: for me, http://192.168.59.103/index.php/admin/ . See [this discussion](http://viget.com/extend/how-to-use-docker-on-os-x-the-missing-guide).


# Legacy notes

See the below for legacy notes.
## Magento API

I'm currently looking into Magento's API capabilities. 

See http://www.magentocommerce.com/api/soap/introduction.html

An introduction to REST integration with Magento  

* http://inchoo.net/magento/magento-rest-and-oauth-intro/
* http://inchoo.net/magento/configure-magento-rest-and-oauth-settings/

## Direct Database to Database copy
### Identify real world Magento databases to work with.

There is a [sample database](http://www.magentocommerce.com/knowledge-base/entry/installing-sample-data-archive-for-magento-ce) that is offered by Magento Commerce. The mysql dump there is 11M with 359 tables.

I expect that real world Magento databases will be far more instructive for us to use to form an idea of integration possibilities. To begin with, we should create a couple of *persistent* stores of our own on [Google's Bitnami](https://bitnami.com/stack/magento/cloud/google). I believe this will help build out our knowledge of the general domain. 

This can serve as a rough starting point but we will need to set up a number of stores and make a list of the things that a migration must carry across. 

Initial list ideas:

* the product catalog
* any SEO configuration or metadata attached to each product (from URL construction to keywords in the HTML header).
* any merchandising information (including assets) attached to the catalog
* user lists

(Please extend this list).

It will probably be the case that we will migrate Magento instances over on a case by case basis, and continually learn as we do so.

## Proceeding

Once we have some quality sample data to work with we can begin to evaluate migration strategies.