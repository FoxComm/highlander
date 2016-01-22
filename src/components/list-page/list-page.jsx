import React, { PropTypes } from 'react';

import ListPageContainer from './list-page-container';
import SearchableList from './searchable-list';

const ListPage = props => {
  return (
    <ListPageContainer {...props}>
      <SearchableList {...props} />
    </ListPageContainer>
  );
};

ListPage.propTypes = {
  ...SearchableList.propTypes,
  ...ListPageContainer.propTypes,
};

export default ListPage;
