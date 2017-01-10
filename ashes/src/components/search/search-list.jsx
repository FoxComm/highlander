
// libs
import React, { PropTypes } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

const SearchList = props => {
  const TotalCounter = makeTotalCounter(state => state.customers.list, customersActions);

  const navLinks = [
    { title: 'Indexing', to: 'search' },
    { title: 'Queries', to: 'queries' },
    { title: 'Stop Words', to: 'stop-words' },
    { title: 'Synonyms', to: 'synonyms' },
    { title: 'Activity Trail', to: 'activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Search Settings"
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

SearchList.propTypes = {
  children: PropTypes.node,
};

export default SearchList;
