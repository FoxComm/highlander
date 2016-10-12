defmodule Solomon.ScopeController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Scope

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
        |> render("show.json", scope: scope)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    scope = Repo.get!(Scope, id)
    render(conn, "show.json", scope: scope)
  end

  def update(conn, %{"id" => id, "scope" => scope_params}) do
    scope = Repo.get!(Scope, id)
    changeset = Scope.update_changeset(scope, scope_params)
    case Repo.update(changeset) do
      {:ok, scope} -> 
        conn
        |> render("show.json", scope: scope)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end

