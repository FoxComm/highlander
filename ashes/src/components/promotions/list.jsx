
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/promotions/list';

type PromotionsListProps = {
  children: Element<*>,
};

const PromotionsList = (props: PromotionsListProps) => {
  const TotalCounter = makeTotalCounter(state => state.promotions.list, actions);
  const addAction = () => transitionTo('promotion-details', {promotionId: 'new'});

  const navLinks = [
    { title: 'Lists', to: 'promotions' },
    { title: 'Activity Trail', to: 'promotions-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Promotions"
      subtitle={<TotalCounter/>}
      addTitle="Promotion"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

PromotionsList.propTypes = {
  children: PropTypes.node,
};

export default PromotionsList;
