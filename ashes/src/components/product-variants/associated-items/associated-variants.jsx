// @flow
import _ from 'lodash';
import React from 'react';

// components
import AssociatedList from './associated-list';
import { IndexLink } from '../../link';

import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  productVariants: Array<ProductVariant>,
  productVariantsState: AsyncState,
}

const AssociatedVariants = (props: Props) => {
  const list = _.map(props.productVariants, (variant: ProductVariant) => {
    // @TODO: backend should include option string in variant.title
    const title = (
      <IndexLink to="product-variant" params={{productVariantId: variant.variantId}}>
        {variant.title}
      </IndexLink>
    );
    return {
      key: `$key-${variant.variantId}`,
      image: variant.image,
      title,
      subtitle: variant.skuCode
    };
  });

  return (
    <AssociatedList
      title="Associated Variants"
      list={list}
      fetchState={props.productVariantsState}
    />
  );
};

export default AssociatedVariants;

