import React, { PropTypes } from 'react';
import _ from 'lodash';

import { AddButton } from '../common/buttons';
import TileItems from './tile-items';

const TileSelector = props => {
  const { emptyMessage, isFetching, items, onAddClick, title } = props;

  return (
    <div className="fc-tile-selector">
      <div className="fc-tile-selector__header">
        <div className="fc-tile-selector__title">
          {title}
        </div>
        <AddButton id="tile-selector-add-btn" onClick={onAddClick} />
      </div>
      <TileItems emptyMessage={emptyMessage} isFetching={isFetching} items={items} />
    </div>
  );
};

TileSelector.propTypes = {
  onAddClick: PropTypes.func,
  emptyMessage: PropTypes.string,
  isFetching: PropTypes.bool,
  items: PropTypes.array,
  title: PropTypes.string.isRequired,
};

TileSelector.defaultProps = {
  onAddClick: _.noop,
  items: [],
};

export default TileSelector;
