import React, { PropTypes } from 'react';

export default class TabView extends React.Component {

  static propTypes = {
    selector: PropTypes.string,
    children: PropTypes.node,
    draggable: PropTypes.bool
  };

  static defaultProps = {
    draggable: true
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      selected: false
    };
  }

  render() {
    let tab = null;
    if (this.props.draggable) {
      tab = (
        <li className="fc-tab">
          <i className="icon-drag-drop"></i>&nbsp;
          {this.props.children}
        </li>
      );
    } else {
      tab = (
        <li className="fc-tab">
          {this.props.children}
        </li>
      );
    }
    return tab;
  }
}
