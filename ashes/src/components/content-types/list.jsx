
// libs
import React from 'react';
import PropTypes from 'prop-types';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer } from '../list-page';

type ContentTypesListProps = {
  children: Element<*>,
};

const ContentTypesList = (props: ContentTypesListProps) => {
  const addAction = () => transitionTo('content-type-details', {contentTypeId: 'new'});

  const navLinks = [
    { title: 'Lists', to: 'content-types' },
    { title: 'Activity Trail', to: 'content-types-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="Content Types"
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
