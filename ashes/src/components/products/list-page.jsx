/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from 'modules/products/list';

type Props = {
  children: any,
};

const ProductsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.products.list, actions);
  const addAction = () => transitionTo('product-details', { productId: 'new', context: 'default' });
  const navLinks = [
    { title: 'Lists', to: 'products' },
    { title: 'Activity Trail', to: 'products-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Products"
      subtitle={<TotalCounter />}
      addTitle="Product"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

ProductsListPage.propTypes = {
  children: PropTypes.node,
};

export default ProductsListPage;
