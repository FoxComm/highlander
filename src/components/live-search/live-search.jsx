import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import SearchOption from './search-option';
import PilledSearch from '../pilled-search/pilled-search';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    updateSearch: PropTypes.func.isRequired,
    selectDown: PropTypes.func.isRequired,
    searchOptions: PropTypes.array,
    state: PropTypes.object.isRequired
  };

  render() {
    return (
      <PilledSearch
        className="fc-live-search fc-col-md-1-1"
        placeholder="Add another filter or keyword search"
        searchButton={<button className="fc-btn">Save Search</button>}
        searchOptions={[{ display: 'Order' }, { display: 'Shipment' }]}
      />
    );
  }
}
