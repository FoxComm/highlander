# Script for populating the database. You can run it as:
#
#     mix run priv/repo/seeds.exs
#
# Inside the script, you can read and write to any of your
# repositories directly:
#
#     Solomon.Repo.insert!(%Solomon.SomeModel{})
#
# We recommend using the bang functions (`insert!`, `update!`
# and so on) as they will fail if something goes wrong.
alias Solomon.Repo
alias Solomon.Account
alias Solomon.System
alias Solomon.Resource
alias Solomon.Action
alias Solomon.Role
alias Solomon.RolePermission
alias Solomon.RoleArchetype
alias Solomon.Scope
alias Solomon.Permission
alias Solomon.AccountRole
alias Solomon.Organization

organizations = for org_name <- ~w(MasterMerchant Merchant1 Merchant2) do
  Repo.insert! %Organization{
    name: org_name,
    kind: "Merchant"
  }
end

accounts = for num <- 1..3 do 
  Repo.insert! %Account{
    ratchet: 3
  }
end

Repo.insert! %System{
  name: "OMS",
  description: "Order management system."
}

resources = 
for resource <- ~w(Orders Returns LineItems) do
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


