// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import WaitAnimation from 'components/common/wait-animation';

// styles
import s from './typeahead.css';

const TypeaheadItems = props => {
  if (props.items.length) {
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
  } else if (props.updating) {
    return <div className={s.items}><WaitAnimation /></div>;
  }

  return <div className={classNames(s.items, s['not-found'])}>No results found.</div>;
};

TypeaheadItems.propTypes = {
  component: PropTypes.func.isRequired,
  updating: PropTypes.bool,
  onItemSelected: PropTypes.func,
  items: PropTypes.array,
};

TypeaheadItems.defaultProps = {
  items: [],
};

export default TypeaheadItems;
