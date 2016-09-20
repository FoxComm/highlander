defmodule Permissions.UserView do
  use Permissions.Web, :view
  alias Permissions.UserView

  def render("index.json", %{users: users}) do
    %{users: render_many(users, UserView, "user.json")}
  end

  def render("show.json", %{user: user}) do
    %{user: render_one(user, UserView, "user.json")}
  end

  def render("user.json", %{user: user}) do
    %{id: user.id,
      email: user.email,
      is_disabled: user.is_disabled,
      disabled_by: user.disabled_by,
      is_blacklisted: user.is_blacklisted,
      blacklisted_by: user.blacklisted_by,
      name: user.name,
      phone_number: user.phone_number
    }
  end

  def render("show_with_account_id.json", %{user: user}) do
    %{user: render_one(user, UserView, "user_with_account_id.json")}
  end

  def render("show_with_account.json", %{user: user}) do
    %{user: render_one(user, UserView, "user_with_access_method.json")}
  end


  def render("user_with_account_id.json", %{user: user}) do
    %{id: user.id,
      email: user.email,
      is_disabled: user.is_disabled,
      disabled_by: user.disabled_by,
      is_blacklisted: user.is_blacklisted,
      blacklisted_by: user.blacklisted_by,
      name: user.name,
      phone_number: user.phone_number,
      account_id: user.account_id
    }
  end

  def render("user_with_access_method.json", %{user: user, account_access_methods: aams}) do
    %{id: user.id,
      email: user.email,
      is_disabled: user.is_disabled,
      disabled_by: user.disabled_by,
      is_blacklisted: user.is_blacklisted,
      blacklisted_by: user.blacklisted_by,
      name: user.name,
      phone_number: user.phone_number,
      account_id: user.account_id, 
      access_methods: render_many(aams, UserView, "account_access_method.json") 
    }
  end

  def render("account_access_method.json", %{aam: aam}) do
    %{name: aam.name,
      hashed_password: aam.name,
      algorithm: aam.algorithm
    }
  end
end
