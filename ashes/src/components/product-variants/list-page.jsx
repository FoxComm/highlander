/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from '../../modules/skus/list';

type Props = {
  children: any,
};

const ProductVariantsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.skus.list, actions);
  const addAction = () => transitionTo('variant-details', { variantId: 'new' });
  const navLinks = [
    { title: 'Lists', to: 'variants' },
    { title: 'Activity Trail', to: 'variants-activity-trail' },
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
