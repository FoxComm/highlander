#Scoped Settings

Currently, we have settings such as Stripe Key, Google analytics Key, and others as
ENV variables. Things like mailchimp use plugins but plugins are not scoped.

This PR will implement an API for settings for tenants which can be configurable
via Ashes.
