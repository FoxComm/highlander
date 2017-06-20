import React from 'react';

import ListPageContainer from './list-page-container';
import SelectableSearchList from './selectable-search-list';

const ListPage = props => {
  return (
    <ListPageContainer {...props}>
      <SelectableSearchList {...props} />
    </ListPageContainer>
  );
};

ListPage.propTypes = {
  ...SelectableSearchList.propTypes,
  ...ListPageContainer.propTypes,
};

export default ListPage;
