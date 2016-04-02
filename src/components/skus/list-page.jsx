/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// actions
import { actions } from '../../modules/skus/list';

type Props = {
  children: any,
};

const SkusListPage = (props: Props, context: Object) => {
  const TotalCounter = makeTotalCounter(state => state.skus.list, actions);
  const addAction = () => transitionTo(context.history, 'sku-details', { skuCode: 'new' });
  const navLinks = [
    { title: 'Lists', to: 'skus' },
    { title: 'Insights', to: 'home' },
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

SkusListPage.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default SkusListPage;
