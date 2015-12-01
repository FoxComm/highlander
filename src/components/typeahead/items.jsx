
import React, { PropTypes } from 'react';

const TypeaheadItems = props => {
  let innerContent = null;

  if (props.items.length > 0) {
    innerContent = props.items.map((item, index) => {
      return (
        <li onClick={() => { props.onItemSelected(item); }} key={`item-${index}`}>
          {React.createElement(props.component, {item})}
        </li>
      );
    });
  } else {
    if (props.updating) {
      innerContent = <li>Loading Results</li>;
    } else {
      innerContent = <li>No results found.</li>;
    }
  }

  return (
    <ul className="fc-typeahead__items">
      {innerContent}
    </ul>
  );
};

TypeaheadItems.propTypes = {
  component: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array.isRequired
};

export default TypeaheadItems;
