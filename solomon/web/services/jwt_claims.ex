defmodule Solomon.JWTClaims do
  import Ecto.Query
  alias Solomon.Repo
  alias Solomon.Account
  alias Solomon.AccountRole
  alias Solomon.Role
  alias Solomon.RolePermission
  alias Solomon.Permission
  alias Solomon.User
  alias Solomon.ScopeService

  def token_claim(account_id) do
    account = Repo.get!(Account, account_id)
    {claims, roles, scope} = get_claims_roles_scope(account_id)
    user = Repo.get_by!(User, account_id: account_id)
    %{
      "aud" => "user",
      "email" => user.email,
      "ratchet" => account.ratchet,
      "id" => account_id,
      "scope" => ScopeService.get_scope_path_by_id!(scope),
      "roles" => roles,
      "name" => user.name,
      "claims" => claims,
      "iss" => "FC",
      "exp" => 0 # TODO : get an exp
    }
  end

  defp get_claims_roles_scope(account_id) do
    Repo.all(
      from ar in AccountRole,
      where: ar.account_id == ^account_id,
      join: r in Role,
      on: ar.role_id == r.id,
      join: rp in RolePermission,
      on: rp.role_id == r.id,
      join: p in Permission,
      on: p.id == rp.permission_id,
      select: {p, r.name}
    )
    |> Enum.unzip
    |> process_perms_and_roles
  end

  defp process_perms_and_roles({permissions, role_names}) do
    {
      Enum.map(permissions, fn p -> %{p.frn => p.actions} end)
      |> Enum.reduce(fn (x, y) -> Map.merge(x, y) end),
      Enum.uniq(role_names),
      Enum.map(permissions, fn p -> p.scope_id end)
      |> Enum.min
    }
  end
end
