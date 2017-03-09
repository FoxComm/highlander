// @flow
import _ from 'lodash';
import React from 'react';

// components
import AssociatedList from './associated-list';
import { IndexLink } from '../../link';

import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  context: string,
  product: {
    id: number,
    attributes: Attributes,
  },
  productVariants: Array<ProductVariant>,
  productVariantsState: AsyncState,
}

const AssociatedProduct = (props: Props) => {
  const title = (
    <IndexLink to="product" params={{productId: props.product.id, context: props.context}}>
      {_.get(props.product.attributes, 'title.v')}
    </IndexLink>
  );

  const { productVariants = [] } = props;

  const list = [{
    key: 'product',
    image: _.get(productVariants, '0.image'),
    title,
    subtitle: `${productVariants.length} variants`,
  }];

  return (
    <AssociatedList
      title="Associated Product"
      list={list}
      fetchState={props.productVariantsState}
    />
  );
};

export default AssociatedProduct;

