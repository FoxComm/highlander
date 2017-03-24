import createProductImage from '@foxcomm/wings/lib/ui/imgix/create-product-image';

const ProductImage = createProductImage(
  'https://tpg-products.imgix.net',
  'tpg-production-images',
  'albums'
);

export default ProductImage;
