import React, { PropTypes } from 'react';

export default class TabListView extends React.Component {

  static propTypes = {
    children: PropTypes.node
  };

  render() {
    return (
      <div className="fc-tab-list">
        <ul className="fc-tab-list__current-tabs">
          {React.Children.map(this.props.children, (child, idx) => {
            return React.cloneElement(child, {
                key: `tab-${idx}`
            });
          })}
        </ul>
        <div className="fc-tab-list__buffer"></div>
      </div>
    );
  }
}
