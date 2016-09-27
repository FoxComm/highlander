defmodule Marketplace.Validation do
  import Ecto
  import Ecto.Changeset

  def validate_phone_number(changeset, field) do
    validate_format(changeset, field, ~r/^(\+?1-?)?\(?\d{3}\)?[-.]?\d{3}[-.]?\d{4}$/)
  end
  
  def validate_uri(changeset, field) do
    validate_format(changeset, field, ~r/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&\/=\\]*)/i)
  end

  def validate_email(changeset, field) do
    validate_format(changeset, field, ~r/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i)
  end

  # examples: Ca, CA, ca
  def validate_US_state(changeset, field) do
    validate_format(changeset, field, ~r/^[a-zA-Z]{2}&/)
  end

  # example: 123456789
  def validate_routing_number(changeset, field) do 
    validate_format(changeset, field, ~r/^\d{9}$/)
  end

  # example: 1234
  def validate_ssn_trailing_four(changeset, field) do
    validate_format(changeset, field, ~r/^\d{4}$/)
  end

  # examples: 12345, 12345-6789
  def validate_postal(changeset, field) do
    validate_format(changeset, field, ~r/^\d{5}(-\d{4})?$/)
  end

  # years are represented with two digits currently
  def validate_date_number(changeset, field) do
    validate_format(changeset, field, ~r/^\d{1,2}$/)
  end
end
