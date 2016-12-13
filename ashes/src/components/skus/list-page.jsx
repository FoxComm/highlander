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

const SkusListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.skus.list, actions);
  const addAction = () => transitionTo('sku-details', { skuId: 'new' });
  const navLinks = [
    { title: 'Lists', to: 'skus' },
    { title: 'Activity Trail', to: 'home' },
  ];

  return (
    <ListPageContainer
      title="SKUs"
      subtitle={<TotalCounter />}
      addTitle="SKU"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

SkusListPage.propTypes = {
  children: PropTypes.node,
};

export default SkusListPage;
