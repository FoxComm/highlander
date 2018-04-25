defmodule Solomon.UserView do
  use Solomon.Web, :view
  alias Solomon.UserView

  def render("index.json", %{users: users}) do
    %{users: render_many(users, UserView, "user.json")}
  end

  def render("show.json", %{user: user}) do
    %{user: render_one(user, UserView, "user.json")}
  end

  def render("user.json", %{user: user}) do
    %{
      id: user.id,
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

  def render("user_with_account_id.json", %{user: user}) do
    %{
      id: user.id,
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
end
