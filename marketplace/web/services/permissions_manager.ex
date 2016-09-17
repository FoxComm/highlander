defmodule Marketplace.PermissionManager do
  import Ecto
  import Ecto.Query
  import HTTPoison
  
  def create_scope do
    permissions_url = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:permissions_url]
    permissions_port = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:permissions_port]
    full_perm_path = "#{permissions_url}:#{permissions_port}"


    HTTPoison.start
    post_body = %{scope: %{source: "Organization"}} 
    |> Poison.encode!
    post_headers = [{'content-type', 'application/json'}]
    
    case HTTPoison.post!("#{full_perm_path}/scopes", post_body, post_headers) do
      %HTTPoison.Response{status_code: 201, body: body} ->
        IO.inspect("created")
        IO.inspect(body)
      {%HTTPoison.Response{status_code: 404}} -> 
        IO.inspect("Not found")
      {%HTTPoison.Error{reason: reason}} -> 
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
      {%{}}
    end
  end

end
