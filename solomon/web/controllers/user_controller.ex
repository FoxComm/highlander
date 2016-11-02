defmodule Solomon.UserController do
  use Solomon.Web, :controller
  alias Ecto.Multi
  alias Solomon.Repo
  alias Solomon.User
  alias Solomon.Account
  alias Solomon.AccountAccessMethod
  alias Solomon.Validation

  def index(conn, _params) do 
    users = Repo.all(User)
    render(conn, "index.json", users: users)
  end

  def create(conn, %{"user" => user_params}) do
    #changeset = User.changeset(%User{}, user_params)

    case Repo.transaction(insert_and_relate(user_params)) do
      {:ok, %{account: account, user: user, account_access_method: aam}} -> 
        user_with_account_id = user
        |> Map.put(:account_id, account.id)
        conn
        |> put_status(:created)
        |> put_resp_header("location", user_path(conn, :show, user))
        |> render("show_with_account_id.json", user: user_with_account_id)
      {:error, failed_operation, failed_value, changes_completedchangeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"id" => id}) do
    user = Repo.get!(User, id)
    |> Repo.preload(:account)
    render(conn, "show_with_account_id.json", user: user)
  end

  def update(conn, %{"id" => id, "user" => user_params}) do
    user = Repo.get!(User, id)
    changeset = User.update_changeset(user, user_params)
                |> check_updated_email(id)
    case Repo.update(changeset) do
      {:ok, user} -> 
        conn
        |> render("show.json", user: user)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end 

  defp insert_and_relate(user_params) do
    account_cs = Account.changeset(%Account{}, %{
      "ratchet" => 0
    })

    Multi.new
    |> Multi.insert(:account, account_cs)
    |> Multi.run(:user, fn %{account: account} ->
      params_with_account = user_params
      |> Map.put("account_id", account.id)
      user_cs = User.changeset(%User{}, params_with_account)
                |> check_new_email
      Repo.insert(user_cs)
    end) 
    |> Multi.run(:account_access_method, fn %{account: account, user: user} -> 
      aam_cs = AccountAccessMethod.changeset(%AccountAccessMethod{}, %{
        "name" => "login",
        "hashed_password" => user_params
        |> Map.fetch!("password"),
        "algorithm" => 1,
        "account_id" => account.id
      })
      Repo.insert(aam_cs)
    end)
  end

  defp check_new_email(changeset) do
    if Validation.email_is_taken(changeset.changes.email) do
      Ecto.Changeset.add_error(changeset, :email, "Email is already taken")
    else
      changeset
    end
  end

  defp check_updated_email(changeset, id) do
    if(
      Map.has_key?(changeset.changes, "email") &&
      Validation.email_is_taken(changeset.changes.email, id)
    ) do
      Ecto.Changeset.add_error(changeset, :email, "Email is already taken")
    else
      changeset
    end
  end
end

