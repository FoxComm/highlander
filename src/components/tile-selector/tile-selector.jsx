import React, { PropTypes } from 'react';
import _ from 'lodash';

import { AddButton } from '../common/buttons';

const TileSelector = props => {
  const tileItems = props.items.map(i => {
    return (
      <div className="fc-tile-selector__item">
        {i}
      </div>
    );
  });

  return (
    <div className="fc-tile-selector">
      <div className="fc-tile-selector__header">
        <div className="fc-tile-selector__title">
          {props.title}
        </div>
        <AddButton onClick={props.onAddClick} />
      </div>
      <div className="fc-tile-selector__items">
        {tileItems}
      </div>
    </div>
  );
};

TileSelector.propTypes = {
  onAddClick: PropTypes.func,
  items: PropTypes.array,
  title: PropTypes.string.isRequired,
};

TileSelector.defaultProps = {
  onAddClick: _.noop,
  items: [],
};

export default TileSelector;
