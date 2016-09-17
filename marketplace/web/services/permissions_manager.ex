defmodule Marketplace.PermissionManager do
  import Ecto
  import Ecto.Query
  import HTTPoison
  
  # This function will create a scope and return an ID if it has been created successfully. 
  # It will return nil if nothing has been created.
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
        case Poison.decode(body) do 
        {:ok, decoded_body} -> 
          IO.inspect("created")
          IO.inspect(decoded_body)
          Map.fetch!(decoded_body, "id") 
        {:error, decoded_body} -> 
          nil
        end 
      %HTTPoison.Response{status_code: 404, body: body} -> 
        case Poison.decode(body) do 
        {:ok, decoded_body} -> 
          IO.inspect("created")
          IO.inspect(decoded_body)
          decoded_body.id 
        {:error, decoded_body} -> 
          nil
        end
      %HTTPoison.Error{reason: reason} -> 
        IO.inspect("ERROR FROM HTTP CLIENT!")
        IO.inspect(reason)
        nil
    end
  end

end
