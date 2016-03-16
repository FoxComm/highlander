
import _ from 'lodash';
import React, { PropTypes } from 'react';

const TypeaheadItems = props => {
  let innerContent = null;

  if (_.isEmpty(props.items)) {
    innerContent = <li className="fc-typeahead__item-not-found">No results found.</li>;
  } else {
    innerContent = props.items.map((item, index) => {
      return (
        <li className="fc-typeahead__item" onMouseDown={() => { props.onItemSelected(item); }} key={`item-${index}`}>
          {React.createElement(props.component, {model: item})}
        </li>
      );
    });
  }

  return (
    <ul className="fc-typeahead__items">
      {innerContent}
    </ul>
  );
};

TypeaheadItems.propTypes = {
  component: PropTypes.func,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array.isRequired
};

export default TypeaheadItems;
