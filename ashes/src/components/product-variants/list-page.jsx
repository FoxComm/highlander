/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from '../../modules/product-variants/list';

type Props = {
  children: any,
};

const ProductVariantsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.productVariants.list, actions);
  const addAction = () => transitionTo('product-variant-details', { productVariantId: 'new' });
  const navLinks = [
    { title: 'Lists', to: 'variants' },
    { title: 'Activity Trail', to: 'product-variants-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Product Variants"
      subtitle={<TotalCounter />}
      addTitle="Product Variant"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

export default ProductVariantsListPage;
