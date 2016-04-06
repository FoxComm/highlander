
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/promotions/list';

type PromotionsListProps = {
  children: Element,
};

type PromotionsListHistory = {
  history: object,
}

const PromotionsList = (props: PromotionsListProps, context: PromotionsListHistory) => {
  const TotalCounter = makeTotalCounter(state => state.promotions.list, actions);
  const addAction = () => transitionTo(context.history, 'promotion-details', {promotionId: 'new'});

  const navLinks = [
    { title: 'Lists', to: 'promotions' },
    { title: 'Insights', to: 'home' },
    { title: 'Activity Trail', to: 'home' }
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

PromotionsList.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default PromotionsList;
