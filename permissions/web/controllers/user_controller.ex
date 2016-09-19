defmodule Permissions.UserController do
  use Permissions.Web, :controller
  alias Ecto.Multi
  alias Permissions.Repo
  alias Permissions.User
  alias Permissions.Account

  def index(conn, _params) do 
    users = Repo.all(User)
    render(conn, "index.json", users: users)
  end

  def create(conn, %{"user" => user_params}) do
    #changeset = User.changeset(%User{}, user_params)

    case Repo.transaction(insert_and_relate(user_params)) do
      {:ok, %{account: account, user: user}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", user_path(conn, :show, user))
        |> render("show.json", user: user)
      {:error, failed_operation, failed_value, changes_completedchangeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: failed_value)
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

  def insert_and_relate(user_params) do
    account_cs = Account.changeset(%Account{}, %{
      "ratchet" => 1
    })

    Multi.new
    |> Multi.insert(:account, account_cs)
    |> Multi.run(:user, fn %{account: account} ->
      params_with_account = user_params
      |> Map.put("account_id", account.id)
      user_cs = User.changeset(%User{}, params_with_account)
      Repo.insert(user_cs)
    end) 
  end
  
  def create_account do
    account_cs = Account.changeset(%Account{}, %{
      "ratchet" => 1
    })
  end 
end

