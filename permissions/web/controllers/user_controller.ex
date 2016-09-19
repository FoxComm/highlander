defmodule Permissions.UserController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.User

  def index(conn, _params) do 
    users = Repo.all(User)
    render(conn, "index.json", users: users)
  end

  def create(conn, %{"user" => user_params}) do
    changeset = User.changeset(%User{}, user_params)

    case Repo.insert(changeset) do
      {:ok, user} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", user_path(conn, :show, user))
        |> render("show.json", user: user)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    user = 
      Repo.get!(User, id)
    render(conn, "show.json", user: user)
  end

  def update(conn, %{"id" => id, "user" => user_params}) do
    user = Repo.get!(User, id)
    changeset = User.update_changeset(user, user_params)
    case Repo.update(changeset) do
      {:ok, user} -> 
        conn
        |> render("show.json", user: user)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

end

