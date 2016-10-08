# Roles/Permissions Design Document

**Author:** Adil Wali  
**Date:** August 30th, 2016

## Overview
As we attack the roles/permissions system (hereby just referred to as _permissions_), we want to be thoughtful about about near-term and long-term implications.  This is an opportunity for us to effectively tackle key notions in our system, such as: organizational users, marketplace functionality, vendor access, and fine-grained classic roles and permissions.

## What might a robust data model look like? 
There are a couple key considerations we'd like to keep in mind as we design this data model.  

  - **Organizations**: We should have some notion of an umbrella construct that can house many users.  We can call this an organization. 
    - Organizations should have some notion of master permissions.  No user in an organization should have more access than the organization at-large has access to.
    - Ideally, the organization umbrella can also contain sensible defaults for users permissions.  Though this is not strictly required. An archetype system can probably get most of this done.
  - **Roles**: Roles will have a collection of permissions.  Ideally, these can be stored as archetypes.  So new users in any organization can have a default role.  And any custom permissions can be branched off of a role.
  - **Resources/Features**: At the highest-level, we want to know if an organizatoin, and subsequently, the users in that organization have access to baseline features (promotions, order management, catalog managemnt, returns, DCG, etc.)  
    - If we take a RESTful approach here, then we have the ability to simply call top-level features resources.  
  - **Actions and Scopes**: We can take a tradional unix-style approach here to have basic actions that exist within certain scopes.
    - In the unix world, we have user, group, and world.  This merits further consideration. (see below)
    - The obvious actions within a resource are the basic CRUD: create, read, update, and delete.  
    - We, theoretically, don't have to slice things that thinly.  We can actually lump together broad things like create, update, and delete into a basic _update_.  This would give us only two permissions: _read_ and _update._

## Other Key Considerations
We want to thoughtful about the implications of this archetecture on the UI and all downstream services.  Some important questions to consider:
  - What is the easiest way for the UI to conditionally display features?  Ie, not showing menu items if the org/user doesn't have access to the whole feature. 
  - How much overhead do we want in each service?  Should it traverse a flat claims list?  Or should there be some kind of summary claim that makes it easier to know if baseline access to the service even exists.
  
## Further Exploration Merited
Things we want to explore further are below.
  - Scoped Actions: We should think more critically the scopes that certain permissions are allowed in.  We already have the notion of both scopes and contexts.  But do we want to structure things more formally?  (This would allows us to have a `CHMOD 777` and a CHMOD `555` equivalent.)
