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
alias Permissions.Permission


Repo.insert! %Account{
  name: "User123",
  ratchet: 3
}

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

actions = 
  for action <- ~w(Read Write) do
    Repo.insert! %Action{
      name: action,
      resource_id: 1
    }
  end

  #Enum.each actions, fn a -> 
  #Repo.insert! a 
  #end 

scopes = 
  for source <- ~w(Organization Project Merchant), parent <- [nil, 1, 2] do
    Repo.insert! %Scope{
      source: source,
      parent_id: parent
    }
  end



for ra <- ~w(Marketer Analyst CSR Manager) do
  Repo.insert! %RoleArchetype{
    name: ra,
    scope_id: 1
  }
end

permissions = 
  for action <- actions, scope <- scopes, resource <- resources do 
    IO.puts("stuff: #{action.id}, #{scope.id}, #{resource.id}.")
    Repo.insert! %Permission{
      action_id: action.id,
      resource_id: resource.id,
      scope_id: scope.id
    }
  end

  #Enum.concat actions, scopes
  #|> insert

insert =  fn (coll) -> 
  Enum.each coll, fn a -> 
    Repo.insert! a
  end
end
