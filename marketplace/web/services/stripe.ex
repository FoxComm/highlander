defmodule Marketplace.Stripe do
  import Ecto
  import Ecto.Query
  import HTTPoison

  # This function will create a new managed account in Stripe Connect and
  # return the account ID for the new account, if successful.
  def create_account(merchant_account, legal_profile_params) do
    HTTPoison.start
    post_headers = [{'Authorization',  "Basic #{stripe_key()}"}]
    post_url = 'https://api.stripe.com/v1/accounts'
    post_body = {:form, [
      'managed': true,
      'country': 'US',
      'legal_entity[dob][day]': String.to_integer(legal_profile_params["business_founded_day"]),
      'legal_entity[dob][month]': String.to_integer(legal_profile_params["business_founded_month"]),
      'legal_entity[dob][year]': String.to_integer(legal_profile_params["business_founded_year"]),
      'legal_entity[first_name]': merchant_account.first_name,
      'legal_entity[last_name]': merchant_account.last_name,
      'legal_entity[business_name]': legal_profile_params["legal_entity_name"],
      'legal_entity[type]': legal_profile_params["legal_entity_type"],
      'external_account[object]': 'bank_account',
      'external_account[country]': 'US',
      'external_account[currency]': 'usd',
      'external_account[routing_number]': legal_profile_params["bank_routing_number"],
      'external_account[account_number]': legal_profile_params["bank_account_number"],
      'legal_entity[address][city]': legal_profile_params["city"],
      'legal_entity[address][line1]': legal_profile_params["address1"],
      'legal_entity[address][postal_code]': legal_profile_params["zip"],
      'legal_entity[address][state]': legal_profile_params["state"],
      'legal_entity[business_tax_id]': legal_profile_params["legal_entity_tax_id"],
      'legal_entity[ssn_last_4]': legal_profile_params["representative_ssn_trailing_four"],
      'tos_acceptance[date]': 1476587489,
      'tos_acceptance[ip]': '67.170.86.209'
    ]} 

    case HTTPoison.post(post_url, post_body, post_headers) do
      {:ok, %HTTPoison.Response{status_code: 200, body: body}} ->
        case Poison.decode(body) do
        {:ok, decoded_body} ->
          account_id = Map.fetch!(decoded_body, "id")
          IO.inspect("Received account from stripe")
          IO.inspect(account_id)
          account_id
        {:error} ->
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