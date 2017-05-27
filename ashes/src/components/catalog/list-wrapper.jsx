/* @flow */

// libs
import React, { Component, Element } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from 'modules/catalog/list';

type Props = { children?: any };

const CatalogsListWrapper = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.catalogs.list, actions);
  const addAction = () => transitionTo('product-details', { productId: 'new', context: 'default' });
  const navLinks = [
    { title: 'Lists', to: 'products' },
    { title: 'Activity Trail', to: 'products-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Catalogs"
      subtitle={<TotalCounter />}
      addTitle="Catalog"
      handleAddAction={addAction}
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default CatalogsListWrapper;
