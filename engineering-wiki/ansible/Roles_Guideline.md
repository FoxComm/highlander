# Quick advices on writing playbooks, roles and so on:
- It is nice to keep roles conditionless (no when clauses)
  It makes them straigtforward and reusable
- So far, there's nothing awful in having lot's of roles (not a role for task of course). 
  We can treat them as immutable logic block, that have some inputs for the things done internally
- One role must perform exactly one obvious thing. If it's not obious what's going on internally - it's bad role
- Ansible has playbooks concept, which is pretty flexible and should be used for all needed logic
  Playbooks can contain other playbooks and nesting level is not limited
- Roles may have default values if they are good defaults (likely, used in pretty much cases). It's bad practice for uncommon defaults otherwise
- In future, we'll rip all variables definitions (defaults too) in separate variable definitions. Thus, roles will be as clear as possible
- There's no much sense to use `sudo: true`/`become: true` for roles. 
  Most of them are intented to be used with root permissions. If not, that can be configured outside. 
  In general, if you need both sude and non-sudo execution in role, you likely need to split it in several roles
