
// libs
import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from '../../modules/content-types/list';

type ContentTypesListProps = {
  children: Element<*>,
};

const ContentTypesList = (props: ContentTypesListProps) => {
  const TotalCounter = makeTotalCounter(state => state.contentTypes.list, actions);
  const addAction = () => transitionTo('content-type-details', {promotionId: 'new'});

  const navLinks = [
    { title: 'Lists', to: 'content-types' },
    { title: 'Activity Trail', to: 'content-types-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Content Types"
      subtitle={<TotalCounter />}
      addTitle="Content Type"
      handleAddAction={addAction}
      navLinks={navLinks}>
      {props.children}
    </ListPageContainer>
  );
};

ContentTypesList.propTypes = {
  children: PropTypes.node,
};

export default ContentTypesList;
