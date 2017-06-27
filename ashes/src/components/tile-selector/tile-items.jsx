import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';

import Spinner from 'components/core/spinner';

const TileItems = props => {
  const { emptyMessage, isFetching, items } = props;
  let content = null;

  if (isFetching) {
    content = <Spinner />;
  } else if (_.isEmpty(items)) {
    content = <div className="fc-tile-selector__empty-message">{emptyMessage}</div>;
  } else {
    content = items.map((item, i) => <div key={i} className="fc-tile-selector__item">{item}</div>);
  }

  return (
    <div className="fc-tile-selector__items">
      {content}
    </div>
  );
};

TileItems.propTypes = {
  emptyMessage: PropTypes.string,
  isFetching: PropTypes.bool,
  items: PropTypes.array,
};

TileItems.defaultProps = {
  emptyMessage: 'No items found.',
  isFetching: false,
  items: [],
};

export default TileItems;
