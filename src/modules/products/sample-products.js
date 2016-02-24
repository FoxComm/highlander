/**
 * @flow
 */
import _ from 'lodash';

export type Product = {
  id: number,
  isActive: boolean,
  attributes: Object,
  variants: Object,
};

export type ProductShadow = {
  id: number,
  productContextId: number,
  productId: number,
  attributes: Object,
};

export type Sku = {
  id: number,
  sku: string,
  attributes: Object,
};

export type SkuShadow = {
  id: number,
  productContextId: number,
  skuId: number,
  attributes: Object,
}

export type ProductResponse = {
  product: Product,
  shadows: Array<ProductShadow>,
};

export type SkuResponse = {
  sku: Sku,
  shadows: Array<SkuShadow>,
};

export function getProductsResponse(): Array<ProductResponse> {
  return products.map(product => {
    const shadows = _.find(productShadows, { productId: product.id });
    return {
      product,
      shadows,
    };
  });
}

export function getProductResponse(id: number): ProductResponse {
  const product = _.find(products, { id: id });
  const shadows = _.find(productShadows, { productId: id });

  return {
    product,
    shadows,
  };
}

export function getSkusResponse(): Array<SkuResponse> {
  return skus.map(sku => {
    const shadows = _.find(skuShadows, { skuId: sku.id });
    return {
      sku,
      shadows,
    };
  });
}

export function getSkuResponse(code: string): SkuResponse {
  const sku = _.find(skus, { sku: code });
  const shadows = _.find(skuShadows, { skuId: sku.id });

  return {
    sku,
    shadows,
  };
}

const products: Array<Product> = [
  {
    id: 1,
    isActive: true,
    attributes: {
      title: {
        type: 'string',
        default: 'Scala Mug',
      },
      description: {
        type: 'string',
        default: 'This is a Scala mug. Why would you want one?',
      },
      images: {
        type: 'images',
        default: ['http://lorempixel.com/75/75/fashion/'],
      },
    },
    variants: {
      default: {
        color: {
          red: 'SCALA-MUG-RED',
          blue: 'SCALA-MUG-BLU',
          black: 'SCALA-MUG-BLK',
        },
      },
    },
  },
  {
    id: 2,
    isActive: true,
    attributes: {
      title: {
        type: 'string',
        default: 'C++ Mug',
      },
      description: {
        type: 'string',
        default: 'This is a C++ mug. It\'s way better than the Scala mug.',
      },
      images: {
        type: 'images',
        default: ['http://lorempixel.com/75/75/fashion/'],
      },
    },
    variants: {
      default: {},
    },
  },
];

const productShadows: Array<ProductShadow> = [
  {
    id: 1,
    productContextId: 1,
    productId: 1,
    attributes: {
      title: 'default',
      description: 'default',
      images: 'default',
    },
  },
  {
    id: 2,
    productContextId: 1,
    productId: 2,
    attributes: {
      title: 'default',
      description: 'default',
      images: 'default',
    },
  },
];

const skus: Array<Sku> = [
  {
    id: 1,
    sku: 'SCALA-MUG-BLK',
    attributes: {
      title: {
        type: 'string',
        default: 'Black Scala Mug',
      },
      price: {
        type: 'price',
        default: {
          price: '1599',
          currency: 'USD',
        },
      },
    },
  },
  {
    id: 2,
    sku: 'SCALA-MUG-RED',
    attributes: {
      title: {
        type: 'string',
        default: 'Red Scala Mug',
      },
      price: {
        type: 'price',
        default: {
          price: '1599',
          currency: 'USD',
        },
      },
    },
  },
  {
    id: 3,
    sku: 'SCALA-MUG-BLU',
    attributes: {
      title: {
        type: 'string',
        default: 'Blue Scala Mug',
      },
      price: {
        type: 'price',
        default: {
          price: '1799',
          currency: 'USD',
        },
      },
    },
  },
];

const skuShadows: Array<SkuShadow> = [
  {
    id: 1,
    productContextId: 1,
    skuId: 1,
    attributes: {
      title: 'default',
      price: 'default',
    },
  },
  {
    id: 2,
    productContextId: 1,
    skuId: 2,
    attributes: {
      title: 'default',
      price: 'default',
    },
  },
  {
    id: 3,
    productContextId: 1,
    skuId: 3,
    attributes: {
      title: 'default',
      price: 'default',
    },
  },
];
