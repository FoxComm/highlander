defmodule Marketplace.PermissionManager do
  import Ecto
  import Ecto.Query
  import HTTPoison
  import Plug.Conn

  # This function will create a scope and return an ID if it has been created successfully.
  # It will return nil if nothing has been created.
  def create_scope(conn) do
    HTTPoison.start
    post_body = %{
      scope: %{
        source: "Organization",
        parent_id: 1
      }}
    |> Poison.encode!
    post_headers = conn.req_headers
    post_cookies = cookies(conn)

    case HTTPoison.post("#{full_perm_path}/scopes",
                        post_body,
                        post_headers,
                        hackney: [cookie: [post_cookies]]) do
      {:ok, %HTTPoison.Response{status_code: 201, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          Map.fetch!(decoded_body, "scope")
          |> Map.fetch!("id")
        {:error, decoded_body} ->
          # TODO: Probably a good idea to write this to a queue.
          # To retry when solomon is back up.
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # Will create a role named "admin" and return an ID
  def create_admin_role_from_scope_id(conn, scope_id) do
    HTTPoison.start
    post_body = %{}
    |> Poison.encode!
    post_headers = conn.req_headers
    post_cookies = cookies(conn)

    case HTTPoison.post("#{full_perm_path}/scopes/#{scope_id}/admin_role",
                        post_body,
                        post_headers,
                        hackney: [cookie: [post_cookies]]) do
      {:ok, %HTTPoison.Response{status_code: 201, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          Map.fetch!(decoded_body, "role")
          |> Map.fetch!("id")
        {:error, decoded_body} ->
          # TODO: Probably a good idea to write this to a queue.
          # To retry when solomon is back up.
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # find the role named "admin" for the with the scope_id and return role_id
  def get_admin_role_from_scope_id(conn, scope_id) do
    HTTPoison.start
    get_headers = conn.req_headers
    get_cookies = cookies(conn)

    case HTTPoison.get("#{full_perm_path}/roles",
                       get_headers,
                       hackney: [cookie: [get_cookies]]) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        case Poison.decode(body) do
          {:ok, decoded_body} ->
            decoded_body
            |> Map.get("roles")
            |> Enum.filter(fn x -> Map.get(x, "scope_id") == scope_id end)
            |> Enum.filter(fn x -> Map.get(x, "name") == "admin" end)
            |> Enum.map(fn x -> Map.get(x, "id") end)
            |> Enum.at(0)
          {:error, _} ->
            nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # grants account with a role over HTTP then returns the role_id
  def grant_account_id_role_id(conn, account_id, role_id) do
    HTTPoison.start
    post_body = %{
      granted_role: %{
        role_id: role_id
      }
    }
    |> Poison.encode!
    post_headers = conn.req_headers
    post_cookies = cookies(conn)

    case HTTPoison.post("#{full_perm_path}/accounts/#{account_id}/granted_roles",
                        post_body,
                        post_headers,
                        hackney: [cookie: [post_cookies]]) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          Map.fetch!(decoded_body, "granted_role")
          |> Map.fetch!("role_id")
        {:error, decoded_body} ->
          # TODO: Probably a good idea to write this to a queue.
          # To retry when solomon is back up.
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  # Will create a user from solomon via HTTP and return the associated account_id
  def create_user_from_merchant_account(conn, ma) do
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
    post_headers = conn.req_headers
    post_cookies = cookies(conn)

    case HTTPoison.post("#{full_perm_path}/users",
                        post_body,
                        post_headers,
                        hackney: [cookie: [post_cookies]]) do
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
  def create_organization_from_merchant_application(conn, ma, scope_id) do
    HTTPoison.start
    post_body = %{
      organization: %{
        name: ma.business_name,
        kind: "merchant",
        scope_id: scope_id,
        parent_id: 1
      }}
    |> Poison.encode!
    post_headers = conn.req_headers
    post_cookies = cookies(conn)

    case HTTPoison.post("#{full_perm_path}/organizations",
                        post_body,
                        post_headers,
                        hackney: [cookie: [post_cookies]]) do
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

  def sign_in_user(conn, org, email, passwd) do
    HTTPoison.start
    post_body = %{
      "user" => %{
        "org" => org,
        "email" => email,
        "password" => passwd
      }
    }
    |> Poison.encode!
    post_headers = [{'content-type', 'application/json'}]

    case HTTPoison.post("#{full_perm_path}/sign_in",
                        post_body,
                        post_headers) do
      {:ok, %HTTPoison.Response{status_code: 200, headers: headers}} ->
        {key, cookie} = Enum.find(headers, fn {x, y} -> x == "set-cookie" end)
        Plug.Conn.put_resp_header(conn, key, cookie)
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

  defp cookies(conn) do
    fetch_cookies(conn).cookies
    |> Map.to_list
    |> Enum.map(fn {k, v} -> k <> "=" <> v end)
    |> Enum.reduce("orig=marketplace", fn (c1, c2) -> c1 <> "; " <> c2 end)
  end

  defp full_perm_path() do
    solomon_url = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:solomon_url]
    solomon_port = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:solomon_port]

    full_perm_path = "#{solomon_url}:#{solomon_port}"
  end
end
