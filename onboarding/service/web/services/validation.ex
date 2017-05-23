defmodule OnboardingService.Validation do
  import Ecto
  import Ecto.Changeset

  def validate_format_code(changeset, field, regex, code) do
    validate_format(changeset, field, regex, message: "validate.format." <> code)
  end
  
  # example: +1-999-999-9999
  def validate_phone_number(changeset, field) do
    validate_format_code(changeset, field, ~r/^\+\d{1,2}-\d{3}-\d{3}-\d{4}$/, "phone")
  end
  
  def validate_uri(changeset, field) do
    validate_format_code(changeset, field, ~r/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&\/=\\]*)/i, "uri")
  end

  def validate_email(changeset, field) do
    validate_format_code(changeset, field, ~r/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i, "email")
  end

  # examples: Ca, CA, ca
  def validate_US_state(changeset, field) do
    validate_format_code(changeset, field, ~r/^[a-zA-Z]{2}$/, "us_state")
  end

  # example: 123456789
  def validate_routing_number(changeset, field) do 
    validate_format_code(changeset, field, ~r/^\d{9}$/, "routing_number")
  end

  # example: 1234
  def validate_ssn_trailing_four(changeset, field) do
    validate_format_code(changeset, field, ~r/^\d{4}$/, "SSN_last_four")
  end

  # examples: 12345, 12345-6789
  def validate_postal(changeset, field) do
    validate_format_code(changeset, field, ~r/^\d{5}(-\d{4})?$/, "postal")
  end

  def validate_day_number(changeset, field) do
    validate_format_code(changeset, field, ~r/^(([0-2]?\d)|(3[01]))$/, "day")
  end

  def validate_month_number(changeset, field) do
    validate_format_code(changeset, field, ~r/^((0?[1-9])|(1[0-2]))$/, "month")
  end

  def validate_year_number(changeset, field) do
    validate_format_code(changeset, field, ~r/^\d{4}$/, "year")
  end

  def validate_required_code(changeset, fields) do
    validate_required(changeset, fields, message: "validate.required")
  end

  def validate_inclusion_code(changeset, field, list) do
    validate_inclusion(changeset, field, list, message: "validate.inclusion")
  end

  def unique_constraint_code(changeset, field, opts \\ []) do
    name = opts[:name]
    unique_constraint(changeset, field, message: "validate.unique", name: name)
  end
end
