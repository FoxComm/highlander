import React, { PropTypes } from 'react';
import classNames from 'classnames';

const generateSearchActions = (selectedIndex, searchOptions) => {
  return searchOptions.map((option, idx) => {
    const klass = classNames('fc-query-component', { 
      'is-active': selectedIndex == idx,
      'is-first': idx == 0
    });

    const action = option.action || 'Search';
    const key = `${option.term}-${idx}`;
    
    return (
      <li className={klass} key={key}>
        <span className='fc-query-component-term'>{option.term}</span>
        <span className='fc-query-component-action'> {action}</span>
      </li>
    );
  });
};

const QueryBuilder = (props) => {
  return (
    <div className='fc-query-builder'>
      <ul className-='fc-query-components'>
        { generateSearchActions(props.selectedIndex, props.searchOptions) }
        <li className='fc-query-component fc-query-component-back'>
          <a onClick={props.onGoBack}>Back</a>
        </li>
      </ul>
    </div>
  );
};

QueryBuilder.propTypes = {
  selectedIndex: PropTypes.number.isRequired,
  searchOptions: PropTypes.array.isRequired,
  onGoBack: PropTypes.func.isRequired
};

export default QueryBuilder;

