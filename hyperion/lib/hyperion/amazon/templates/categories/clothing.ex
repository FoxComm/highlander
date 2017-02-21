defmodule Hyperion.Amazon.Templates.Categories.Clothing do

  # TODO: Add variation data processing

  def template_string do
    """
    <ClothingAccessories>
      <VariationData>
          <Parentage><%= @parentage %></Parentage>
          <%= if @parentage == "child" do %>
            <Size><%= @size %></Size>
            <Color><%= @color %></Color>
            <VariationTheme>SizeColor</VariationTheme>
          <% end %>
        </VariationData>
      <ClassificationData>
        <Department><%= @department %></Department>
        <ClothingType><%= @clothingtype %></ClothingType>
      </ClassificationData>
    </ClothingAccessories>
    """
  end
end
