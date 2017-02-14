defmodule Hyperion.Amazon.Templates.Categories.Books do
  def template_string do
    """
    <Books>
      <ProductType>
        <BooksMisc>
          <Author><%= author %></Author>
          <Binding><%= bindingtypes %></Binding>
          <Language><%= language %></Language>
        </BooksMisc>
      </ProductType>
    </Books>
    """
  end
end