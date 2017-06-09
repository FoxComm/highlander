
// libs
import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/content-types/list';

const ContentTypesListPage = (props) => {
  const TotalCounter = makeTotalCounter(state => state.contentTypes.list, actions);

  const navLinks = [
    { title: 'Lists', to: 'content-types' },
    { title: 'Activity Trail', to: 'content-types-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Content Types"
      subtitle={<TotalCounter />}
      addTitle="Content Type"
      handleAddAction={ () => transitionTo('content-types-new') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

ContentTypesListPage.propTypes = {
  children: PropTypes.node,
};

export default ContentTypesListPage;
