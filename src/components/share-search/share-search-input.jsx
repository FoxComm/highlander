import React, { PropTypes } from 'react';
import _ from 'lodash';

import AdminResult from '../orders/admin-result';
import quickSearchTypeahead from '../typeahead/quick-search-typeahead';

const getState = state => _.get(state, 'orders.adminSearch');

const ShareSearchInput = props => {
  const Component = quickSearchTypeahead(getState, props.actions);
  return (
    <Component
      itemComponent={AdminResult}
      onItemSelected={_.noop}
      placeholder="Name or email..." />
  );
};

ShareSearchInput.propTypes = {
  actions: PropTypes.object.isRequired,
};

export default ShareSearchInput;
