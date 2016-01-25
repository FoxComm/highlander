import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';

export default class ElasticSearchOptions extends React.Component {
  constructor(props, ...args) {
    super(props, ...args);

    this.state = {
      isVisible: props.isVisible
    };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({ ...this.state, isVisible: nextProps.isVisible });
  }

  static propTypes = {
    isVisible: PropTypes.bool.isRequired,
    options: PropTypes.array.isRequired,
  };

  render() {
    const items = this.props.options.map((option, idx) => {
      return (
        <MenuItem 
          isFirst={idx == 0} 
          clickAction={option.action}>
          {option.title}
        </MenuItem>
      );
    });

    if (this.state.isVisible) {
      return <Menu>{items}</Menu>;
    } else {
      return <div></div>;
    }
  }
};
