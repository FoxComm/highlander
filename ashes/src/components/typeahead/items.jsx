// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// styles
import s from './typeahead.css';

const TypeaheadItems = props => {
  let innerContent = null;

  if (_.isEmpty(props.items)) {
    innerContent = <li className={s['not-found']}>No results found.</li>;
  } else {
    innerContent = props.items.map(item => (
      <li
        className={s.item}
        onMouseDown={() => props.onItemSelected(item)}
        key={`item-${item.id || item.text}`}
      >
        {React.createElement(props.component, {model: item})}
      </li>
    ));
  }

  return (
    <ul className={s.items}>
      {innerContent}
    </ul>
  );
};

TypeaheadItems.propTypes = {
  component: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array,
};

TypeaheadItems.defaultValues = {
  items: [],
};

export default TypeaheadItems;
