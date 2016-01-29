import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

import Typeahead from './typeahead';

export default function quickSearchTypeahead(getState, actions) {

  const mapStateToProps = state => {
    return { search: getState(state) };
  };

  const mapDispatchToProps = {
    doSearch: actions.doSearch
  };
  
  @connect(mapStateToProps, mapDispatchToProps)
  class QuickSearchTypeahead extends Component {
    static propTypes = {
      doSearch: PropTypes.func.isRequired,
      itemComponent: PropTypes.func.isRequired,
      onItemSelected: PropTypes.func.isRequired,
      placeholder: PropTypes.string,
    };

    static defaultProps = {
      placeholder: null,
    };

    get isFetching() {
      return _.get(this.props, 'search.isFetching', false);
    }

    get results() {
      return _.get(this.props, 'search.result.rows', []);
    }

    render() {
      return (
        <Typeahead
          component={this.props.itemComponent}
          fetchItems={this.props.doSearch}
          isFetching={this.isFetching}
          items={this.results}
          onItemSelected={this.props.onItemSelected}
          placeholder={this.props.placeholder} />
      );
    }
  }

  return QuickSearchTypeahead;
}

