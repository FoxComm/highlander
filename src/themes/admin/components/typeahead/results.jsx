'use strict';

import React from 'react/addons';

export default class TypeaheadResults extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      component: null,
      results: props.store.getState()
    };
  }

  onComponentDidMount() {
    this.setState({
      component: React.addons.createFragment({'component': this.props.component})
    });
  }

  cloneComponent(props) {
    return React.cloneElement(this.state.component.name, props);
  }

  render() {
    let innerContent = null;

    if (this.state.results.length > 0) {
      innerContent = this.state.results.map((result) => {
        return <li>{this.cloneComponent({result: result})}</li>;
      });
    } else {
      innerContent = <li>No results found.</li>;
    }

    return (
      <ul className="typeahead-results">
        {innerContent}
      </ul>
    );
  }
}

TypeaheadResults.propTypes = {
  store: React.PropTypes.object,
  component: React.PropTypes.element
};
