import React, { PropTypes } from 'react';

export default class TypeaheadItems extends React.Component {

  static propTypes = {
    store: PropTypes.object,
    component: PropTypes.func.isRequired,
    showItems: PropTypes.bool,
    updating: PropTypes.bool,
    onItemSelected: PropTypes.func,
    items: PropTypes.array.isRequired
  };

  createComponent(props) {
    return React.createElement(this.props.component, props);
  }

  render() {
    let innerContent = null;

    if (this.props.items.length > 0) {
      innerContent = this.props.items.map((item, index) => {
        return (
          <li onClick={() => { this.props.onItemSelected(item); }} key={`item-${index}`}>
            {this.createComponent({item})}
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
      <ul className={`fc-typeahead-items ${this.props.showItems ? 'show' : ''}`}>
        {innerContent}
      </ul>
    );
  }
}

