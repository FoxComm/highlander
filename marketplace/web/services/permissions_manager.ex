defmodule Marketplace.PermissionManager do
  import Ecto
  import Ecto.Query
  import HTTPoison
  
  # This function will create a scope and return an ID if it has been created successfully. 
  # It will return nil if nothing has been created.
  def create_scope do
    HTTPoison.start
    post_body = %{scope: %{source: "Organization"}} 
    |> Poison.encode!
    post_headers = [{'content-type', 'application/json'}]
    
    case HTTPoison.post("#{full_perm_path}/scopes", post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 201, body: body}} ->
        case Poison.decode(body) do 
        {:ok, decoded_body} -> 
          Map.fetch!(decoded_body, "scope")
          |> Map.fetch!("id")
        {:error, decoded_body} -> 
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} -> 
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # Will create an organization from solomon via HTTP and return an ID
  def create_user_from_merchant_account(ma) do
    HTTPoison.start
    first_name = Map.get(ma, "first_name", "FirstName")
    last_name = Map.get(ma, "last_name", "LastName")
    email = Map.get(ma, "email_address", "donkey@donkey.com")
    phone_number = Map.get(ma, "phone_number", "415-673-3553")
    password = Map.fetch!(ma, "password")
    post_body = %{user: %{name: "#{first_name} #{last_name}", 
        email: email,
        is_disabled: false,
        is_blacklisted: false,
        phone_number: phone_number,
        password: password
      }}
    |> Poison.encode!
    post_headers = [{'content-type', 'application/json'}]

    case HTTPoison.post("#{full_perm_path}/users", post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 201, body: body}} ->
        case Poison.decode(body) do 
        {:ok, decoded_body} -> 
          Map.fetch!(decoded_body, "user")
          |> Map.fetch!("account_id")
        {:error, decoded_body} -> 
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} -> 
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # Will create a user in solomon via HTTP and return an ID
  def create_organization_from_merchant_application(ma, scope_id) do
    HTTPoison.start
    post_body = %{organization: %{name: ma.name, kind: "merchant", scope_id: scope_id}}
    |> Poison.encode!
    post_headers = [{'content-type', 'application/json'}]

    case HTTPoison.post("#{full_perm_path}/organizations", post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 201, body: body}} ->
        case Poison.decode(body) do 
        {:ok, decoded_body} -> 
          Map.fetch!(decoded_body, "organization")
          |> Map.fetch!("id")
        {:error, decoded_body} -> 
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} -> 
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  defp full_perm_path() do
    solomon_url = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:solomon_url]
    solomon_port = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:solomon_port]

    full_perm_path = "#{solomon_url}:#{solomon_port}"
  end
end
