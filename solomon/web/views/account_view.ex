defmodule Solomon.AccountView do
  use Solomon.Web, :view
  alias Solomon.AccountView

  def render("index.json", %{accounts: accounts}) do
    %{accounts: render_many(accounts, AccountView, "account.json")}
  end

  def render("show.json", %{account: account}) do
    %{account: render_one(account, AccountView, "account.json")}
  end

  def render("account.json", %{account: account}) do
    %{id: account.id, ratchet: account.ratchet}
  end
end
