// @flow

// libs
import _ from 'lodash';
import React from 'react';

// styles
import s from './typeahead.css';

type Props = {
  component: Function,
  updating: boolean,
  onItemSelected: (item: Object) => void,
  items: Array<*>,
};

const TypeaheadItems = (props: Props) => {
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

TypeaheadItems.defaultProps = {
  items: [],
};

export default TypeaheadItems;
