defmodule Permissions.AccountView do
  use Permissions.Web, :view
  alias Permissions.AccountView

  def render("index.json", %{accounts: accounts}) do
    %{accounts: render_many(accounts, AccountView, "account.json")}
  end

  def render("show.json", %{account: account}) do
    %{account: render_one(account, AccountView, "account.json")}
  end

  def render("account.json", %{account: account}) do
    %{id: account.id,
      name: account.name
    }
  end
end
