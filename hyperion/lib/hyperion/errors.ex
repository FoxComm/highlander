defmodule NotAllowed do
  defexception message: "Can not verify JWT header"
end

defmodule AmazonError do
  defexception message: "Amazon error occured"
end

defmodule AmazonCredentialsError do
  defexception message: "Credentials not found"
end

defmodule PhoenixError do
  defexception message: "Phoenix error occured"
end
