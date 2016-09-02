# Script for populating the database. You can run it as:
#
#     mix run priv/repo/seeds.exs
#
# Inside the script, you can read and write to any of your
# repositories directly:
#
#     Permissions.Repo.insert!(%Permissions.SomeModel{})
#
# We recommend using the bang functions (`insert!`, `update!`
# and so on) as they will fail if something goes wrong.
alias Permissions.Repo
alias Permissions.Account
alias Permissions.System
alias Permissions.Resource
alias Permissions.Action
alias Permissions.Role
alias Permissions.RoleArchetype
alias Permissions.Scope


Repo.insert! %Account{
  name: "User123",
  ratchet: 3
}

Repo.insert! %System{
  name: "OMS",
  description: "Order management system."
}

Repo.insert! %Resource{
  name: "Orders",
  description: "Access to orders",
  system_id: 1
}

for action <- ~w(Read Write) do 
  Repo.insert! %Action{
  name: action,
  resource_id: 1
  }
end

c = nil
for source <- ~w(Organization Project Merchant) do
  Repo.insert! %Scope{
    source: source,
    parent: c
  }
  c = (c || 0) + 1
end

for ra <- ~w(Marketer Analyst CSR Manager) do
  Repo.insert! %RoleArchetype{
    name: ra,
    scope_id: 1
  }
end

