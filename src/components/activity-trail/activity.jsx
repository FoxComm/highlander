
import React, { PropTypes } from 'react';

export default class Activity extends React.Component {

  static propTypes = {
    activity: PropTypes.shape({
      type: PropTypes.string.isRequired,
    }).isRequired,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      expanded: false
    };
  }

  render() {
    const { activity } = this.props;

    return (
      <li className="fc-activity">
        {activity.type}
      </li>
    );
  }
}
