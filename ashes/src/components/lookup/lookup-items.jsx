// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// helpers
import { prefix } from '../../lib/text-utils';


const prefixed = prefix('fc-lookup');

const LookupItems = props => {
  return (
    <ul className={prefixed('items')}>
      {getBody(props)}
    </ul>
  );
};

const getBody = ({component, query, items, activeIndex, onSelect, notFound}) => {
  if (_.isEmpty(items)) {
    return <li className={prefixed('item-not-found')}>{notFound}</li>;
  }

  return _.map(items, (item, index) => (
    <li key={`lookup-${index}`}
        className={classNames(prefixed('item'), {'_active': index === activeIndex})}
        onMouseDown={() => onSelect(parseInt(index))}>
      {React.createElement(component, {
        query,
        model: item
      })}
    </li>
  ));
};

LookupItems.propTypes = {
  component: PropTypes.func.isRequired,
  query: PropTypes.string.isRequired,
  items: PropTypes.array.isRequired,
  onSelect: PropTypes.func.isRequired,
  notFound: PropTypes.string.isRequired,
};

export default LookupItems;
