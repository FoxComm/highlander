/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from '../../modules/products/list';

type Props = {
  children: any,
};

const ProductsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.products.list, actions);
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
      handleAddAction={_.noop}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

export default ProductsListPage;
