// @flow

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import styles from './plugin.css';

import { PageTitle } from 'components/section-title';
import ObjectFormInner from 'components/object-form/object-form-inner';
import ObjectForm from 'components/object-form/object-form';
import SaveCancel from 'components/common/save-cancel';
import WaitAnimation from 'components/common/wait-animation';
import PluginState from './plugin-state';

import * as PluginsActions from 'modules/plugins';
import type { UpdateSettingsPayload } from 'modules/plugins';
import type { Attribute, Attributes } from 'paragons/object';

type Props = {
  fetchSettings: (name: string) => Promise,
  updateSettings: (name: string, payload: UpdateSettingsPayload) => Promise,
  changeState: (name: string, payload: UpdateStatePayload) => Promise,
  params: {
    name: string,
  },
  currentPlugin: {
    settings: Object,
  },
  isFetching: boolean,
}

const pluginName = (props: Props): string => {
  return props.params.name;
};

function guessType(value) {
  const typeOf = typeof value;
  switch (typeOf) {
    case 'string':
    case 'number':
      return typeOf;
    case 'boolean':
      return 'bool';
    default:
      return 'string';
  }
}

function attributesFromSettings(settings: Object): Attributes {
  return _.reduce(settings, (acc:Attributes, value: any, key: string) => {
    acc[key] = {
      t: guessType(value),
      v: value
    };
    return acc;
  }, {});
}

function settingsFromAttributes(attributes: Attributes): Object {
  return _.reduce(attributes, (acc: Object, attr: Attribute, key: string) => {
    acc[key] = attr.v;
    return acc;
  }, {});
}

function mapStateToProps(state) {
  return {
    currentPlugin: state.plugins.currentPlugin,
    isFetching: _.get(state.asyncActions, 'fetchPluginSettings.inProgress', null),
  };
}

type State = {
  settings: Object,
}

class Plugin extends Component {
  props: Props;

  state: State = {
    settings: {},
  };

  componentDidMount() {
    this.props.fetchSettings(pluginName(this.props));
  }

  componentWillReceiveProps(nextProps: Props) {
    console.log(nextProps);
    if (pluginName(nextProps) != this.pluginName) {
      this.props.fetchSettings(pluginName(nextProps));
    }
    if (nextProps.currentPlugin != null && !_.isEqual(nextProps.currentPlugin.settings, this.state.settings)) {
      this.setState({
        settings: nextProps.currentPlugin.settings,
      });
    }
  }

  get pluginName(): string {
    return pluginName(this.props);
  }

  get attributes(): Attributes {
    return attributesFromSettings(this.state.settings);
  }

  get title(): string {
    return `${this.pluginName} Configuration`
  }

  @autobind
  handleChange(attributes: Attributes) {
    this.setState({
      settings: settingsFromAttributes(attributes),
    });
  }

  @autobind
  handleSave() {
    this.props.updateSettings(this.pluginName, {
      settings: this.state.settings,
    });
  }

  @autobind
  handleStateChange(newState: string) {
    if (this.props.currentPlugin.state !== newState) {
      this.props.changeState(this.pluginName, {state: newState});
    }
  }

  get content(): Element {
    if (this.props.isFetching !== false) {
      return <WaitAnimation/>;
    } else {
      return (
        <div className="fc-grid fc-grid-no-gutter">
          <div className="fc-col-md-3-5">
            <ObjectForm
              title={this.title}
              attributes={this.attributes}
              onChange={this.handleChange}
            />
          </div>
          <div className="fc-col-md-2-5">
            <PluginState
              currentValue={this.props.currentPlugin.state}
              handleDropdownChange={(value) => this.handleStateChange(value)}
            />
          </div>
        </div>
      );
    }
  }


  render() {
    const title = `Plugin ${this.pluginName}`;
    return (
      <div>
        <PageTitle title={title}>
          <SaveCancel
            cancelTo="plugins"
            onSave={this.handleSave}
          />
        </PageTitle>
        <div styleName="content">
          {this.content}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, PluginsActions)(Plugin);
