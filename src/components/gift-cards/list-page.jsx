
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/gift-cards/list';

const GiftCardsListPage = (props) => {
  const TotalCounter = makeTotalCounter(state => state.giftCards.list, actions);

  const navLinks = [
    { title: 'Lists', to: 'gift-cards' },
    { title: 'Insights', to: 'home' },
    { title: 'Activity Trail', to: 'gift-cards-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Gift Cards"
      subtitle={<TotalCounter />}
      addTitle="Gift Card"
      handleAddAction={ () => transitionTo('gift-cards-new') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

GiftCardsListPage.propTypes = {
  children: PropTypes.node,
};

export default GiftCardsListPage;
