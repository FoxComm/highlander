defmodule Permissions.PermissionClaimService do
  import Ecto
  import Ecto.Query
  alias Ecto.Multi
  alias Ecto.Query
  alias Permissions.Permission
  alias Permissions.Claim
  alias Permissions.Repo

  def insert_permission(params) do
    perm_changeset = Permission.changeset(%Permission{}, params)
    Multi.new
    |> Multi.insert(:permission, perm_changeset)
    |> Multi.run(:claim, fn %{permission: permission} -> create_claim_changeset(permission) end)
  end

  defp create_claim_changeset(created_permission) do
    claim_frn = Repo.all(
      from permission in Permission,
      join: resource in assoc(permission, :resource),
      join: action in assoc(permission, :action), #to remove
      join: scope in assoc(permission, :scope),
      where: permission.id == ^created_permission.id,
      select: %{
        id: permission.id,
        resource_name: resource.name,
        action_name: action.name,
        scope_id: scope.id
      },
      limit: 1
    )
    |> construct_frn
    claim_changeset = Claim.changeset_from_frn(%Claim{}, %{"frn" => claim_frn})
    Repo.insert(claim_changeset)
  end

  defp construct_frn(full_permission) do
    fp = List.first(full_permission)
    "Fox/#{fp.scope_id}/#{fp.resource_name}/#{fp.action_name}"
  end
end
