defmodule Marketplace.Stripe do
  import Ecto
  import Ecto.Query
  import HTTPoison

  # This function will create a new managed account in Stripe Connect and
  # return the account ID for the new account, if successful.
  def create_account do
    HTTPoison.start
    post_headers = [{'Authorization',  "Basic #{stripe_key()}"}]
    post_url = 'https://api.stripe.com/v1/accounts'
    post_body = {:form, [managed: true, country: 'US']}

    case HTTPoison.post(post_url, post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          account_id = Map.fetch!(decoded_body, "id")
          IO.inspect("Received account from stripe")
          IO.inspect(account_id)
          account_id
        {:error, decoded_body} ->
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT CONNECTING TO STRIPE!")
        IO.inspect(reason)
        nil
    end
  end

  def verify_account do
    HTTPoison.start
    post_headers = [{'Authorization',  "Basic #{stripe_key()}"}]
    post_url = 'https://api.stripe.com/v1/accounts/acct_19566hH3onkspVzM'
    post_body = {:form, [
      'legal_entity[dob][day]': 10,
      'legal_entity[dob][month]': 1,
      'legal_entity[dob][year]': 1986,
      'legal_entity[first_name]': 'Jenny',
      'legal_entity[last_name]': 'Rosen',
      'legal_entity[type]': 'individual',
      'external_account[object]': 'bank_account',
      'external_account[country]': 'US',
      'external_account[currency]': 'usd',
      'external_account[routing_number]': '110000000',
      'external_account[account_number]': '000123456789',
      'tos_acceptance[date]': 1476587489,
      'tos_acceptance[ip]': '67.170.86.209'
    ]} 

    case HTTPoison.post(post_url, post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          IO.inspect("Updated account in stripe")
          nil
        {:error, decoded_body} ->
          nil
        end
      {:error, %HTTPoison.Error{reason: reason}} ->
        IO.inspect("ERROR FROM HTTP CLIENT CONNECTING TO STRIPE!")
        IO.inspect(reason)
        nil
    end
  end

  defp stripe_key() do
    raw_key = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:stripe_private_key]
    Base.encode64(raw_key)
  end
end