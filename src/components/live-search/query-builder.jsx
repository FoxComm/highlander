import React, { PropTypes } from 'react';
import classNames from 'classnames';

const generateSearchActions = (selectedIndex, searchOptions) => {
  return searchOptions.map((option, idx) => {
    const klass = classNames('fc-query-component', { 
      'fc-active-component': selectedIndex == idx 
    });
    const action = option.action || 'Search';
    const key = `${option.term}-${idx}`;
    
    return (
      <li className={klass} key={key}>
        <div className='contents'>
          <strong>{option.term}</strong> {action}
        </div>
      </li>
    );
  });
};

const QueryBuilder = (props) => {
  return (
    <div className='fc-query-builder'>
      <ul className-='fc-query-components'>
        { generateSearchActions(props.selectedIndex, props.searchOptions) }
        <li>
          <button onClick={props.onGoBack}>Back</button>
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

