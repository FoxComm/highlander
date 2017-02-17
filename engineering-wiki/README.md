# FoxCommerce Engineering Wiki

Welcome to the FoxCommerce Engineering Wiki. Here you can find information about the overall engineering team, as well as the individual projects. Look below to find your way around.

## Design Documents

* [System Architecture](design/architecture)
* [Activity Trail](design/activity-trail)
* [Discounts](design/discounts)
  * [Discount Algebra](design/discounts/discount_algebra.md)
* [Inventory](design/inventory)
* [Product Model](design/product)
  * [PIM / Merch Object Model](design/objects)
* [Production](design/production)

## Architecture

#### Customer's Simplified View of FoxCommerce Platform

It's good to keep in mind that our customers will understand a simplified version of what's actually happening at FoxComm. They will mostly be interacting with the Store Admin to manage their catalog and merchandising, and their design & dev teams will be building their public storefront, most likely based on the Firebird starter theme.

```
                                            ╮
                    ┌───────────────────┐   │
┌─────────────┐     │ Public Storefront │   ├ Frontend
│ Store Admin │     └─────────┬─────────┘   │
└──────┬──────┘               │             ╯
       └──────────┬───────────┘
                  │                         ╮
         ┌────────┴────────────┐            │
         │ FoxComm Backend API │            ├ Backend
         └─────────────────────┘            │
                                            ╯
```


#### FoxCommerce Platform Architecture

More detail on what's going on behind the scenes.

```
                                                               ╮
                            ┌──────────────────────────────┐   │
┌─────────────────────┐     │ Public Storefront [Firebird] │   ├ Frontend
│ Store Admin [Ashes] │     └──────────────┬───────────────┘   │
└─────────┬───────────┘                    │                   ╯
          └───────────────┬────────────────┘
                          │
               ┌──────────┴──────────┐                         ╮
               │ API Gateway [nginx] │                         │
               └──────────┬──────────┘                         │
                          │                                    │
             ┌────────────┴────────────┐                       │
      ┌──────┴──────┐         ┌────────┴──────────┐            │
      │ Phoenix API │         │ ElasticSearch API │            ├ Backend
      └──────┬──────┘         └──────────┬────────┘            │
          ╭──┴─╮                         ↑ update indices      │
          │    │     ┌───────┐     ┌─────┴───────┐             │
          │ DB ├────→│ Kafka │────→│ Green River │             │
          │    │     └───────┘     └─────────────┘             │
          ╰────╯                                               ╯
```



## Core FoxCommerce Platform Components

### Frontend UI

See also [customer implementation & theming](customer-implementation/theming.md) and [developing frontend applications](development/setup.md#developing-frontend-applications)

* [Ashes Store Admin UI](https://github.com/FoxComm/Ashes) — React.js based UI for Phoenix
* [Firebird Demo Storefront](https://github.com/FoxComm/firebird) — Isomorphic node/javascript React web store
* [API.js library](https://github.com/FoxComm/api-js) — Simple API client for interacting with Phoenix API; framework-agnostic javascript


### Backend Services

* [Phoenix core backend API](https://github.com/FoxComm/phoenix-scala) — see also [wiki docs](phoenix/README.md)
* [Green River](https://github.com/FoxComm/green-river) — Kafka Consumers to capture and act on Phoenix data change events, eg. updating ElasticSearch indices

See [Developing backend services](development/setup.md#developing-backend-applications)


### Libraries & Utilities

* [Tabernacle](https://github.com/FoxComm/tabernacle) — Core provisioning tool
* [Money](https://github.com/FoxComm/money) — A money abstraction in GoLang
* [Wings](https://github.com/FoxComm/wings) — Shared UI components & tools
