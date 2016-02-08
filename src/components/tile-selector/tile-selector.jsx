import React, { PropTypes } from 'react';
import _ from 'lodash';

import { Button } from '../common/buttons';

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
      <div className="fc-tile-selector__items">
        {tileItems}
      </div>
    </div>
  );
};

TileSelector.propTypes = {
  items: PropTypes.array,
};

TileSelector.defaultProps = {
  items: [],
};

export default TileSelector;
