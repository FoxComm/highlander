defmodule Hyperion.Amazon.Templates.SubmitProductFeed do
  alias Hyperion.Amazon.Templates.Categories.Books
  alias Hyperion.Amazon.Templates.Categories.ClothingAccessories
  use Timex

  def template_string do
    """
    <?xml version="1.0" ?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn- envelope.xsd">
    <Header>
      <DocumentVersion>1.01</DocumentVersion>
      <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
    </Header>
    <MessageType>Product</MessageType>
    <PurgeAndReplace><%= purge_and_replace %></PurgeAndReplace>
    <Message>
      <MessageID>1</MessageID>
      <OperationType>Update</OperationType>
      <%= for p <- products do %>
        <Product>
          <SKU><%= p[:code]%></SKU>
          <StandardProductID>
            <%= cond do %>
              <% Keyword.has_key?(p, :asin) -> %>
                <Type>ASIN</Type>
                <Value><%= p[:asin]%></Value>
              <% Keyword.has_key?(p, :upc) -> %>
                <Type>UPC</Type>
                <Value><%= p[:upc]%></Value>
              <% Keyword.has_key?(p, :ean) -> %>
                <Type>EAN</Type>
                <Value><%= p[:ean]%></Value>
              <% Keyword.has_key?(p, :isbn) -> %>
                <Type>ISBN</Type>
                <Value><%= p[:isbn]%></Value>
              <% true -> %>
                <% nil %>
            <% end %>
          </StandardProductID>
          <ProductTaxCode><%= p[:tax_code] %></ProductTaxCode>
          <LaunchDate><%= Hyperion.Amazon.Templates.SubmitProductFeed.format_date_time(p[:activefrom]) %></LaunchDate>
          <DescriptionData>
            <Title><%= p[:title] %></Title>
            <Description><%= HtmlSanitizeEx.strip_tags(p[:description]) %></Description>
            <%= for t <- p[:tags] do %>
              <SearchTerms><%= t %></SearchTerms>
            <% end %>
      <!--  <BulletPoint>made in Italy</BulletPoint>
            <BulletPoint>500 thread count</BulletPoint>
            <BulletPoint>plain weave (percale)</BulletPoint>
            <BulletPoint>100% Egyptian cotton</BulletPoint>
            <Manufacturer>Peacock Alley</Manufacturer>
            <ItemType></ItemType>
            <IsGiftWrapAvailable>false</IsGiftWrapAvailable>
            <IsGiftMessageAvailable>false</IsGiftMessageAvailable> -->
          </DescriptionData>
          <ProductData>
            <%= cond do %>
              <% p[:category] == "Books" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.books_category(p) %>
              <% p[:category] == "Clothing" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.clothing_category(p) %>
              <% true -> %>
                <% nil %>
            <% end %>
          </ProductData>
        </Product>
      <% end %>
    </Message>
    </AmazonEnvelope>
    """
  end

  def format_date_time(dt_str) do
    {:ok, dt} = Timex.Parse.DateTime.Parser.parse(dt_str, "{ISO:Extended:Z}")
    Timex.format(dt, "%Y-%m-%dT%H:%M:%S+00:00", :strftime) |> elem(1)
  end
end