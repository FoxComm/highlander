'use strict';

import React from 'react';

export default class TypeaheadResults extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      results: props.store.getState()
    };
  }

  componentDidMount() {
    let
      store = this.props.store,
      storeName = store.storeName;

    this[`onChange${storeName}`] = this.onStoreChange;
    store.listenToEvent('change', this);
  }

  componentWillUnmount() {
    this.props.store.stopListeningToEvent('change', this);
  }

  createComponent(props) {
    return React.createElement(this.props.component, props);
  }

  onStoreChange() {
    this.setState({results: this.props.store.getState()});
  }

  render() {
    let innerContent = null;

    if (this.state.results.length > 0) {
      innerContent = this.state.results.map((result) => {
        return (
          <li onClick={() => { this.props.onItemSelected(result); }} key={result.id}>
            {this.createComponent({model: result})}
          </li>
        );
      });
    } else {
      if (this.props.updating) {
        innerContent = <li>Loading Results</li>;
      } else {
        innerContent = <li>No results found.</li>;
      }
    }

    return (
      <ul className={`fc-typeahead-results ${this.props.showResults ? 'show' : ''}`}>
        {innerContent}
      </ul>
    );
  }
}

TypeaheadResults.propTypes = {
  store: React.PropTypes.object,
  component: React.PropTypes.func,
  showResults: React.PropTypes.bool,
  updating: React.PropTypes.bool,
  onItemSelected: React.PropTypes.func
};
