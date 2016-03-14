/** Libs */
import React, { PropTypes } from 'react';

/** Components */
import Initials from '../users/initials';

/**
 * Watchers typeahead suggested list item component
 */
const WatcherTypeaheadItem = props => {
  const { name, email } = props.model;

  return (
    <div className="fc-watcher-typeahead__item">
      <div className="fc-watcher-typeahead__item-icon">
        <Initials {...props.model} />
      </div>
      <div className="fc-watcher-typeahead__item-name">
        {name}
      </div>
      <div className="fc-watcher-typeahead__item-email">
        {email}
      </div>
    </div>
  );
};

/**
 * WatcherTypeaheadItem component expected props types
 */
WatcherTypeaheadItem.propTypes = {
  model: PropTypes.shape({
    name: PropTypes.string,
    firstName: PropTypes.string,
    lastName: PropTypes.string,
    email: PropTypes.string,
  })
};

export default WatcherTypeaheadItem;
