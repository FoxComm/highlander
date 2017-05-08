import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

/**
 * Toggle
 */
export default class Toggle extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      on: 'yes',
      off: 'no',
      value: !!this.props.value
    };
  }

  handleClick(event) {
    event.preventDefault();
    this.setState({
      value: !this.state.value
    }, () => {
      if (this.props.onToggle) {
        this.props.onToggle(this.state.value);
      }
    });
  }

  render() {
    var classes = classNames({
      'fc-ui-toggle': true,
      'fc-ui-toggle_on': this.state.value
    });
    return (
      <div className={classes} onClick={this.handleClick.bind(this)}>
        <div className="fc-ui-toggle__inner">
          <div className="fc-ui-toggle__label">{this.state.on}</div>
          <div className="fc-ui-toggle__label">{this.state.off}</div>
        </div>
      </div>
    );
  }
}

Toggle.propTypes = {
  value: PropTypes.bool,
  onToggle: PropTypes.func
};
