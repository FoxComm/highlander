// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// styles
import s from './typeahead.css';

const TypeaheadItems = props => {
  let innerContent = null;

  if (props.noResults) {
    return <div className={classNames(s.items, s['not-found'])}>No results found.</div>;
  }

  if (!_.isEmpty(props.items)) {
    return (
      <ul className={s.items}>
        {props.items.map(item => (
          <li
            className={s.item}
            onMouseDown={() => props.onItemSelected(item)}
            key={`item-${item.key || item.id}`}
          >
            {React.createElement(props.component, { model: item, query: props.query })}
          </li>
        ))}
      </ul>
    );
  }

  return null;
};

TypeaheadItems.propTypes = {
  component: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array,
  noResults: PropTypes.bool,
};

TypeaheadItems.defaultProps = {
  items: [],
  noResults: false,
};

export default TypeaheadItems;
