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
alias Permissions.RolePermission
alias Permissions.RoleArchetype
alias Permissions.Scope
alias Permissions.Permission
alias Permissions.AccountRole


accounts = for num <- 1..3 do 
  Repo.insert! %Account{
    name: "User#{num}",
    ratchet: 3
  }
end

Repo.insert! %System{
  name: "OMS",
  description: "Order management system."
}

resources = 
for resource <- ~W(Orders Returns LineItems) do
  Repo.insert! %Resource{
    name: resource,
    description: "Access to #{resource}",
    system_id: 1
  }
end

actions = ~w(Read Write)

scopes = 
  for source <- ~w(Organization Project Merchant), parent <- [nil, 1, 2] do
    Repo.insert! %Scope{
      source: source,
      parent_id: parent
    }
  end

roles = 
for r <- ~w(Marketer Analyst CSR Manager), scope <- scopes do
  Repo.insert! %Role{
    name: r,
    scope_id: scope.id
  }
end


permissions = 
  for scope <- scopes, resource <- resources do 
    Repo.insert! %Permission{
      actions: actions,
      resource_id: resource.id,
      scope_id: scope.id,
      frn: "Fox:OMS:Donkey:1.5"
    }
  end

role_permissions = 
  for role <- roles, permission <- permissions do 
    Repo.insert! %RolePermission{
      role_id: role.id, 
      permission_id: permission.id
    }
  end

account_roles = 
  for account <- accounts, role <- roles do
    Repo.insert! %AccountRole{
      account_id: account.id,
      role_id: role.id
    }
  end


