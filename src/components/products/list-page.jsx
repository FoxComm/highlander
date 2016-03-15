/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from '../../modules/products/list';

type Props = {
  children: any,
};

const ProductsListPage = (props: Props, context: Object) => {
  const TotalCounter = makeTotalCounter(state => state.products.list, actions);
  const addAction = () => transitionTo(context.history, 'new-product');
  const navLinks = [
    { title: 'Lists', to: 'products' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: '' },
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

ProductsListPage.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default ProductsListPage;
