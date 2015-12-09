import React, { PropTypes } from 'react';
import classnames from 'classnames';
import TabView from './tab';

export default class EditableTabView extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    defaultValue: PropTypes.string.isRequired,
    editing: PropTypes.bool
  };

  static defaultProps = {
    editing: false
  };

  get className() {
    return classnames({ '_editing': this.props.editing });
  }

  get tabContent() {
    if (this.props.editing) {
      return (
        <div className="fc-tab__edit-content fc-form-field">
          <input
            type="text"
            placeholder="Name your search"
            value=""
          />
          <div className="fc-tab__edit-content-close">
            <a onClick={() => console.log("Not Yet")}>
              &times;
            </a>
          </div>
        </div>
      );
    } else {
      return this.props.defaultValue;
    }
  }

  render() {
    return (
      <TabView className={this.className} {...this.props}>
        {this.tabContent}
      </TabView>
    );
  }
}