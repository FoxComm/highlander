defmodule Permissions.ScopeController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Scope

  def index(conn, _params) do 
    scopes = Repo.all(Scope)
    render(conn, "index.json", scopes: scopes)
  end

  def create(conn, %{"scope" => scope_params}) do
    changeset = Scope.changeset(%Scope{}, scope_params)

    case Repo.insert(changeset) do
      {:ok, scope} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", fc_scope_path(conn, :show, scope))
        |> render("scope.json", scope: scope)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

end

