import React, { PropTypes } from 'react';
import classNames from 'classnames';

const generateSearchActions = (selectedIndex, searchOptions, selectOptionFunc) => {
  return searchOptions.map((option, idx) => {
    const klass = classNames('fc-query-component', { 
      'is-active': selectedIndex == idx,
      'is-first': idx == 0
    });

    const action = option.action || 'Search';
    const key = `${option.term}-${idx}`;
    
    return (
      <li className={klass} key={key} onClick={() => selectOptionFunc(idx)}>
        <span className='fc-query-component-term'>{option.term}</span>
        <span className='fc-query-component-action'> : {action}</span>
      </li>
    );
  });
};

const QueryBuilder = (props) => {
  return (
    <div className='fc-query-builder'>
      <ul className-='fc-query-components'>
        { generateSearchActions(props.selectedIndex, props.searchOptions, props.selectOption) }
        <li className='fc-query-component fc-query-component-back' onClick={props.onGoBack}>
          Back
        </li>
      </ul>
    </div>
  );
};

QueryBuilder.propTypes = {
  selectOption: PropTypes.func.isRequired,
  selectedIndex: PropTypes.number.isRequired,
  searchOptions: PropTypes.array.isRequired,
  onGoBack: PropTypes.func.isRequired,
};

export default QueryBuilder;

