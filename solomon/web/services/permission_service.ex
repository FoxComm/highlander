defmodule Solomon.PermissionClaimService do
  import Ecto
  import Ecto.Query
  alias Ecto.Changeset
  alias Ecto.Multi
  alias Ecto.Query
  alias Solomon.Permission
  alias Solomon.Claim
  alias Solomon.Repo
  alias Solomon.Resource
  alias Solomon.System

  def insert_permission(params) do
    perm_changeset = Permission.changeset(%Permission{}, params)
    |> create_claim_changeset
    #Multi.new
    #|> Multi.insert(:permission, perm_changeset)
    #|> Multi.run(:claim, fn %{permission: permission} -> create_claim_changeset(permission) end)
    
  end

  defp create_claim_changeset(perm_changeset) do
    resource_id = Changeset.get_change(perm_changeset, :resource_id)
    scope_id = Changeset.get_change(perm_changeset, :scope_id)
    actions = Changeset.get_change(perm_changeset, :actions)
    claim_frn = Repo.all(
      from resource in Resource,
      join: system in System,
      where: resource.system_id == system.id,
      select: %{
        resource_name: resource.name,
        system_name: system.name,
        actions: resource.actions
      },
      limit: 1
    )
    |> List.first
    |> Map.merge(%{scope_id: scope_id})
    |> construct_frn
    changeset_with_claim = Permission.changeset(perm_changeset, %{"frn" => claim_frn})

    Multi.new
    |> Multi.insert(:permission, changeset_with_claim)
  end

  defp construct_frn(fp) do
    case fp do 
      fp when is_nil fp -> empty_frn
      fp ->  "frn:#{fp.system_name}:#{fp.resource_name}:#{fp.scope_id}"
    end
  end

  defp empty_frn() do
    "frn:none"
  end

end
