'use strict';

import React from 'react/addons';

export default class TypeaheadResults extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      results: props.store.getState()
    };
  }

  createComponent(props) {
    return React.createElement(this.props.component, props);
  }

  render() {
    let innerContent = null;

    if (this.state.results.length > 0) {
      innerContent = this.state.results.map((result) => {
        return <li>{this.createComponent({result: result})}</li>;
      });
    } else {
      innerContent = <li>No results found.</li>;
    }

    return (
      <ul className={`typeahead-results ${this.state.results.length > 0 ? 'show' : null}`}>
        {innerContent}
      </ul>
    );
  }
}

TypeaheadResults.propTypes = {
  store: React.PropTypes.object,
  component: React.PropTypes.element
};
