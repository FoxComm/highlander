# Customer Implementation Theming

Implementing a FoxCommerce store must be:

* Delightful
* Understandable
* Powerful
* Extensible

Which means:

* Easy to get going, great "out of box" developer experience
  * clear documentation, easy dev workflow
  * all basic functions "just work" and follow modern best practices for customer UX
* Clear inheritance / override pattern for layout & style, icons, base ui components [buttons, qty counter], complex components [cart, checkout and their constituents]


To accomplish this we need to break apart the current FireBird into different concerns. FoxComm Storefront will be built on 3 main layers:

1. Lowest level, a minimal API library to abstract and perform all the direct interaction with the FxC API
  - framework-agnostic javascript, not React
1. Largest codebase should be the Storefront SDK, isomorphic React
  - as much as possible stateless components
  - sane defaults for everything we think is best practices for ecomm
  - core ui components, from simple [buttons] to complex [checkout]
  - all components should be overridable in the next layer [customer theme]
1. Top level, the React/Node of client, which should be as thin as possible. Contains all templates, layout, style, "glue" to pull everything together [state, routing, importing all root modules], final build process [gulp or webpack], etc. We should provide a starter store that customers can either directly fork, or use as an example to get going.



```
      ┌────────────────────────────────────┐        ╮
      │ Customer Storefront Implementation │        ├ Firebird / Demo store
      └──────────────────┬─────────────────┘        ╯
                         │                          ╮
╭────────────────────────┴────────────────────────╮ │
  ┌────────────────────┐  ┌────────────────────┐    │
  │ Reducers / Modules ├──┤ Complex Components │    │
  └────────────────────┘  └───────┬────────────┘    ├ Storefront SDK
                            ┌─────┴────┐            │
                            │ Base UI  │            │
                            └──────────┘            │
╰────────────────────────┬────────────────────────╯ │
                         │                          ╯
                 ┌───────┴────────┐                 ╮
                 │ API.js Library │                 ├ API helper library
                 └───────┬────────┘                 ╯
                         │             
               ┌─────────┴───────────┐
               │ FoxComm Backend API │
               └─────────────────────┘
```
