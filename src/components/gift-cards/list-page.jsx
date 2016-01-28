
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from '../../route-helpers';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/gift-cards/list';

const GiftCardsListPage = (props, context) => {
  const TotalCounter = makeTotalCounter(state => state.giftCards.list, actions);

  const navLinks = [
    { title: 'Lists', to: 'gift-cards' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: 'gift-cards-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Gift Cards"
      subtitle={<TotalCounter url="gift_cards_search_view/_search" />}
      addTitle="Gift Card"
      handleAddAction={ () => transitionTo(context.history, 'gift-cards-new') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

GiftCardsListPage.propTypes = {
  children: PropTypes.node,
};

GiftCardsListPage.contextTypes = {
  history: PropTypes.object.isRequired,
};

export default GiftCardsListPage;
