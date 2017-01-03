---
layout: hero
title: Next-Generation eCommerce.  Today.
id: home
---

<section class="light home-section">
  <div class="marketing-row">
    <div class="marketing-col">
      <h3>API-First</h3>
      <p>Fox is built for modern applications, with a 100% of the application surface-area covered by the API.  All of the power of the underlying platform is accessible by you.</p>
      <p>Whether you require a POS, a full eCommerce site, or simply a mobile app, multiple UIs can be built atop Fox with ease.</p>
    </div>
    <div class="marketing-col">
      <h3>Modular</h3>
      <p>A platform built from the ground-up to empower you to pick and choose what you need.  Focus on what makes your business special; leave the rest to us.</p>
      <p>Migrating from a legacy platform?  Reduce risk and increase value along the way by incrementally adopting Fox.</p>
    </div>
    <div class="marketing-col">
      <h3>Open Data & Event Architecture</h3>
      <p>We don't make assumptions about the rest of your technology stack, so you can develop new features in any language you choose.  You can also easily port over legacy plugins and add-ons to Fox.</p>
      <p>Every update to all major objects in the Fox system is published to the event stream.  You can consume multiple topics of the stream, and act on them however you wish.</p>
    </div>
  </div>
</section>
<hr class="home-divider" />
<section class="home-section">
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

* [Ashes Store Admin UI](https://github.com/FoxComm/Ashes) — React.js based UI for Phoenix
* [Firebird Demo Storefront](https://github.com/FoxComm/firebird) — Isomorphic node/javascript React web store
* [API.js library](https://github.com/FoxComm/api-js) — Simple API client for interacting with Phoenix API; framework-agnostic javascript


### Backend Services

* [Phoenix core backend API](https://github.com/FoxComm/phoenix-scala) — see also [wiki docs](phoenix/README.md)
* [Green River](https://github.com/FoxComm/green-river) — Kafka Consumers to capture and act on Phoenix data change events, eg. updating ElasticSearch indices

See [Developing backend services](development/setup.md#developing-backend-applications)


### Libraries & Utilities

* [Provisioning all the Shit](https://github.com/FoxComm/prov-shit) — Core provisioning tool
* [Money](https://github.com/FoxComm/money) — A money abstraction in GoLang
* [Wings](https://github.com/FoxComm/wings) — Shared UI components & tools
</section>
<hr class="home-divider" />
<section class="home-bottom-section">
  <div class="buttons-unit">
    <a href="docs/hello-world.html" class="button">API Docs</a>
    <a href="tutorial/tutorial.html" class="button">Tutorials</a>
  </div>
</section>
